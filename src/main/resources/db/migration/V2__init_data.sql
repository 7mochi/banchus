INSERT INTO users (id, username, email, password_md5, country, restricted, privileges)
VALUES (1, 'BanchoBot', 'bancho@osupe.ru', '098f6bcd4621d373cade4e832627b4f6', 118, false, 0),
       (3, 'test', 'test@gmail.com', '098f6bcd4621d373cade4e832627b4f6', 118, false, 0),
       (4, 'test2', 'test2@gmail.com', '098f6bcd4621d373cade4e832627b4f6', 13, true, 0);

INSERT INTO channels (id, name, topic, read_privileges, write_privileges, auto_join, created_at, updated_at)
VALUES (UUID(), '#osu', 'General discussion.', 0, 0, 1, NOW(), NOW()),
       (UUID(), '#lobby', 'General multiplayer lobby chat.', 0, 1 << 0, 1, NOW(), NOW()),
       (UUID(), '#announce', 'Announcements from the server.', 1 << 0, 1 << 9, 1, NOW(), NOW()),
       (UUID(), '#help', 'Help and support.', 1 << 0, 1 << 0, 1, NOW(), NOW()),
       (UUID(), '#staff', 'General discussion for staff members.', (1 << 7 | 1 << 9 | 1 << 13 | 1 << 30),
        (1 << 7 | 1 << 9 | 1 << 13 | 1 << 30),
        1, NOW(), NOW()),
       (UUID(), '#dev', 'General discussion for developers.', 1 << 30, 1 << 30, 1, NOW(), NOW());

INSERT INTO stats (user_id, gamemode)
VALUES (1, 0),
       (1, 1),
       (1, 2),
       (1, 3);

INSERT INTO stats (user_id, gamemode)
VALUES (3, 0),
       (3, 1),
       (3, 2),
       (3, 3);

INSERT INTO stats (user_id, gamemode)
VALUES (4, 0),
       (4, 1),
       (4, 2),
       (4, 3);
