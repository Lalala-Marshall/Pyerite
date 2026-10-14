#!/usr/bin/env python3
"""Project one EveSDE 3.0 item_db.sqlite into a language-specific Pyerite database.

EveSDE 3.0 ships a single wide sqlite plus texts.zip. Room still expects the
EveSDE 2.0 column set (one database per language). This copies the template
CREATE TABLE / index SQL from the bundled database and fills language columns
from the 3.0 wide columns. It stops if a column the app reads has no source.
"""

from __future__ import annotations

import argparse
import json
import shutil
import sqlite3
import zipfile
from pathlib import Path

LANG_CODES = ("de", "en", "es", "fr", "ja", "ko", "ru", "zh")
MAP_NAME_STEMS = ("solarSystemName", "regionName", "constellationName")
TEXTS_TABLE = "sde_texts"
TYPE_LOOKUP_COLUMNS = {
    "blueprintTypeName": ("blueprintTypeID", "name"),
    "blueprintTypeIcon": ("blueprintTypeID", "icon_filename"),
    "typeName": ("typeID", "name"),
    "typeIcon": ("typeID", "icon_filename"),
}

# Active-language column -> source column in the 3.0 table. {lang} is zh or en.
ACTIVE_SOURCE = {
    ("agents", "agent_name"): "{lang}_name",
    ("categories", "name"): "{lang}_name",
    ("divisions", "name"): "{lang}_name",
    ("dogmaAttributeCategories", "name"): "{lang}_name",
    ("dogmaAttributeCategories", "description"): "{lang}_description",
    ("dogmaAttributes", "name"): "attribute_key",
    ("dogmaAttributes", "display_name"): "{lang}_name",
    ("dogmaAttributes", "tooltipDescription"): "{lang}_tooltip_description",
    ("dogmaAttributes", "unitName"): "unit_{lang}_name",
    ("dogmaEffects", "display_name"): "{lang}_name",
    ("dogmaEffects", "description"): "{lang}_description",
    ("factions", "name"): "{lang}_name",
    ("factions", "description"): "{lang}_description",
    ("factions", "shortDescription"): "{lang}_short_description",
    ("groups", "name"): "{lang}_name",
    ("marketGroups", "name"): "{lang}_name",
    ("metaGroups", "name"): "{lang}_name",
    ("npcCorporations", "name"): "{lang}_name",
    ("npcCorporations", "description"): "{lang}_description",
    ("planetSchematics", "name"): "{lang}_name",
    ("stations", "stationName"): "{lang}_name",
    ("traits", "content"): "{lang}_content",
    ("typeMaterials", "output_material_name"): "output_material_{lang}_name",
    ("typeSkillRequirement", "typename"): "type_{lang}_name",
    ("typeSkillRequirement", "category_name"): "category_{lang}_name",
    ("types", "name"): "{lang}_name",
    ("types", "group_name"): "group_{lang}_name",
    ("types", "category_name"): "category_{lang}_name",
    ("wormholes", "name"): "{lang}_name",
    ("wormholes", "description"): "{lang}_description",
}

# Tables Room opens that EveSDE 2.0 did not ship. 3.0 includes them; the
# column list must stay identical to the entity.
NATIVE_ROOM_COLUMNS = {
    "certificateSkills": (
        "certificateID",
        "skillID",
        "basic",
        "standard",
        "improved",
        "advanced",
        "elite",
    ),
    "masteries": ("typeid", "masteryLevel", "certificateID"),
}


def quote_ident(name: str) -> str:
    return '"' + name.replace('"', '""') + '"'


def table_columns(con: sqlite3.Connection, table: str) -> list[str]:
    return [row[1] for row in con.execute(f"PRAGMA table_info({quote_ident(table)})")]


def user_tables(con: sqlite3.Connection) -> list[str]:
    return [
        row[0]
        for row in con.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table' "
            "AND name NOT LIKE 'sqlite_%' ORDER BY name"
        )
    ]


def create_sql(con: sqlite3.Connection, table: str) -> str:
    row = con.execute(
        "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
        (table,),
    ).fetchone()
    if row is None or not row[0]:
        raise SystemExit(f"Template database has no CREATE TABLE for {table}")
    return str(row[0])


def index_sqls(con: sqlite3.Connection, table: str) -> list[str]:
    return [
        str(row[0])
        for row in con.execute(
            "SELECT sql FROM sqlite_master WHERE type = 'index' AND tbl_name = ? "
            "AND sql IS NOT NULL ORDER BY name",
            (table,),
        )
    ]


def load_texts(texts_zip: Path) -> dict[str, str]:
    with zipfile.ZipFile(texts_zip) as archive:
        json_name = next((name for name in archive.namelist() if name.endswith("texts.json")), None)
        if json_name is None:
            raise SystemExit(f"Expected texts.json in {texts_zip}, found {archive.namelist()!r}")
        payload = json.loads(archive.read(json_name))
    if not isinstance(payload, dict) or not payload:
        raise SystemExit(f"{texts_zip} did not contain a text map")
    return {str(key): value if isinstance(value, str) else str(value) for key, value in payload.items()}


def map_name_source(column: str, lang: str) -> str | None:
    if column in MAP_NAME_STEMS:
        return f"{lang}_name"
    for stem in MAP_NAME_STEMS:
        prefix = stem + "_"
        if column.startswith(prefix):
            code = column[len(prefix) :]
            if code in LANG_CODES:
                return f"{code}_name"
    return None


def source_expr(table: str, column: str, lang: str, src_columns: set[str]) -> str:
    if column in src_columns:
        return f"src.{quote_ident(column)}"
    if table == "types" and column == "description":
        desc_id = f"{lang}_desc_id"
        if desc_id not in src_columns:
            raise SystemExit(f"types.{desc_id} is missing; cannot fill description")
        return (
            f"(SELECT content FROM {TEXTS_TABLE} "
            f"WHERE id = src.{quote_ident(desc_id)})"
        )
    if column in TYPE_LOOKUP_COLUMNS:
        fk, dest = TYPE_LOOKUP_COLUMNS[column]
        if fk not in src_columns:
            raise SystemExit(f"{table}.{fk} is missing; cannot fill {column}")
        return (
            f"(SELECT {quote_ident(dest)} FROM types "
            f"WHERE type_id = src.{quote_ident(fk)})"
        )
    mapped = map_name_source(column, lang)
    if mapped is None:
        template = ACTIVE_SOURCE.get((table, column))
        mapped = template.format(lang=lang) if template else None
    if mapped is None or mapped not in src_columns:
        raise SystemExit(
            f"No source for {table}.{column} in the {lang} database "
            f"(looked for {mapped or 'a same-named column'})"
        )
    return f"src.{quote_ident(mapped)}"


def needs_type_lookup(columns: list[str]) -> bool:
    return any(column in TYPE_LOOKUP_COLUMNS for column in columns)


def rebuild_table(
    dest: sqlite3.Connection,
    template: sqlite3.Connection,
    table: str,
    lang: str,
) -> None:
    src_columns = set(table_columns(dest, table))
    old_columns = table_columns(template, table)
    if old_columns == table_columns(dest, table):
        return
    src_name = f"__src_{table}"
    dest.execute(f"ALTER TABLE {quote_ident(table)} RENAME TO {quote_ident(src_name)}")
    dest.execute(create_sql(template, table))
    select_list = ", ".join(source_expr(table, column, lang, src_columns) for column in old_columns)
    column_list = ", ".join(quote_ident(column) for column in old_columns)
    dest.execute(
        f"INSERT INTO {quote_ident(table)} ({column_list}) "
        f"SELECT {select_list} FROM {quote_ident(src_name)} AS src"
    )
    dest.execute(f"DROP TABLE {quote_ident(src_name)}")
    for sql in index_sqls(template, table):
        dest.execute(sql)


def assert_filled(dest: sqlite3.Connection, sql: str, label: str) -> None:
    count = dest.execute(sql).fetchone()[0]
    if count <= 0:
        raise SystemExit(f"Projection produced no {label}")


def verify_language_db(dest: sqlite3.Connection, template: sqlite3.Connection, lang: str) -> None:
    assert_filled(dest, "SELECT COUNT(*) FROM dogmaAttributes WHERE name = 'requiredSkill1'", "requiredSkill1")
    assert_filled(dest, "SELECT COUNT(*) FROM types WHERE name IS NOT NULL AND name != ''", f"{lang} type names")
    assert_filled(
        dest,
        "SELECT COUNT(*) FROM types WHERE description IS NOT NULL AND description != ''",
        f"{lang} type descriptions",
    )
    assert_filled(
        dest,
        "SELECT COUNT(*) FROM stations WHERE stationName IS NOT NULL AND stationName != ''",
        "station names",
    )
    assert_filled(
        dest,
        "SELECT COUNT(*) FROM regions WHERE regionName IS NOT NULL AND regionName != ''",
        "region names",
    )
    blueprint_rows = dest.execute("SELECT COUNT(*) FROM blueprint_manufacturing_output").fetchone()[0]
    if blueprint_rows:
        assert_filled(
            dest,
            "SELECT COUNT(*) FROM blueprint_manufacturing_output "
            "WHERE blueprintTypeName IS NOT NULL AND blueprintTypeName != ''",
            "blueprint names",
        )
    dest_tables = set(user_tables(dest))
    for table in user_tables(template):
        if table not in dest_tables:
            if table == "celestialNames":
                continue
            raise SystemExit(f"Projected database dropped template table {table}")
        if table_columns(dest, table) != table_columns(template, table):
            raise SystemExit(f"Projected {table} columns do not match the Room template")
        expected_indexes = [
            row[0]
            for row in template.execute(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = ? AND sql IS NOT NULL",
                (table,),
            )
        ]
        actual_indexes = {
            row[0]
            for row in dest.execute(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = ? AND sql IS NOT NULL",
                (table,),
            )
        }
        missing = [name for name in expected_indexes if name not in actual_indexes]
        if missing:
            raise SystemExit(f"Projected {table} is missing indexes {missing}")
    for table, columns in NATIVE_ROOM_COLUMNS.items():
        actual = tuple(table_columns(dest, table))
        if actual != columns:
            raise SystemExit(f"{table} columns {actual} != Room columns {columns}")


def project_language_db(
    source_sqlite: Path,
    texts_zip: Path,
    template_sqlite: Path,
    output_sqlite: Path,
    lang: str,
) -> None:
    if lang not in ("zh", "en"):
        raise SystemExit(f"Unsupported language {lang!r}")
    if not source_sqlite.is_file():
        raise SystemExit(f"Missing EveSDE sqlite: {source_sqlite}")
    if not texts_zip.is_file():
        raise SystemExit(f"Missing texts.zip: {texts_zip}")
    if not template_sqlite.is_file():
        raise SystemExit(f"Missing Room schema template: {template_sqlite}")

    output_sqlite.parent.mkdir(parents=True, exist_ok=True)
    if output_sqlite.exists():
        output_sqlite.unlink()

    print(f"[+] Projecting {lang} database from {source_sqlite.name}")
    texts = load_texts(texts_zip)
    shutil.copy2(source_sqlite, output_sqlite)
    dest = sqlite3.connect(output_sqlite)
    template_uri = template_sqlite.resolve().as_uri() + "?mode=ro"
    template = sqlite3.connect(template_uri, uri=True)
    failed = False
    try:
        dest.execute("PRAGMA foreign_keys = OFF")
        dest.execute(f"CREATE TABLE {TEXTS_TABLE} (id TEXT PRIMARY KEY, content TEXT)")
        dest.executemany(
            f"INSERT INTO {TEXTS_TABLE} (id, content) VALUES (?, ?)",
            texts.items(),
        )
        template_tables = user_tables(template)
        source_tables = set(user_tables(dest))
        ordered = sorted(
            template_tables,
            key=lambda name: (
                0 if name == "types" else 2 if needs_type_lookup(table_columns(template, name)) else 1,
                name,
            ),
        )
        for table in ordered:
            if table not in source_tables:
                if table == "celestialNames":
                    print("[i] celestialNames is not in EveSDE 3.0; the app does not query it")
                    continue
                raise SystemExit(f"EveSDE 3.0 is missing table {table}")
            print(f"[+] {lang} {table}")
            rebuild_table(dest, template, table, lang)
        dest.execute(f"DROP TABLE {TEXTS_TABLE}")
        dest.commit()
        verify_language_db(dest, template, lang)
        dest.execute("VACUUM")
        print(f"[+] Wrote {output_sqlite}")
    except BaseException:
        failed = True
        raise
    finally:
        dest.close()
        template.close()
        if failed:
            output_sqlite.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--texts", type=Path, required=True)
    parser.add_argument("--template", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--lang", choices=("zh", "en"), required=True)
    args = parser.parse_args()
    project_language_db(args.source, args.texts, args.template, args.output, args.lang)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
