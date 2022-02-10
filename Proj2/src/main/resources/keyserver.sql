DROP TABLE IF EXISTS User;
CREATE TABLE User (
    user_id INTEGER PRIMARY KEY ASC,
    user_username TEXT UNIQUE NOT NULL,
    user_pubkey TEXT NOT NULL
);