import sqlite3

c = sqlite3.connect(r"D:\Coding\Pyerite\app\src\main\assets\db\item_db_zh.sqlite")
for row in c.execute("SELECT type, name, sql FROM sqlite_master WHERE tbl_name='divisions'"):
    print(row)
print("---")
for row in c.execute("PRAGMA table_info(divisions)"):
    print(row)
