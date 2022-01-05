DROP TABLE IF EXISTS User;
CREATE TABLE User (
    user_id INTEGER PRIMARY KEY ASC,
    user_username TEXT UNIQUE NOT NULL,
    user_pubkey TEXT NOT NULL
);

DROP TABLE IF EXISTS Post;
CREATE TABLE Post (
    post_id INTEGER PRIMARY KEY ASC,
    post_date INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)),
    post_ciphered TEXT NOT NULL,
    post_content TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES User
);
