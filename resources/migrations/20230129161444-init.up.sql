CREATE TABLE IF NOT EXISTS todos(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    description TEXT,
    status TEXT,
    user_id INTEGER FOREIGN KEY
);

CREATE TABLE IF NOT EXISTS users(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id INTEGER,
    first_name TEXT,
    last_name TEXT,
    username TEXT
);

CREATE TABLE IF NOT EXISTS chats(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id INTEGER,
    type TEXT
);
