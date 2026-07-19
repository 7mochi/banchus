INSERT INTO channels (id, name, description, read_privileges, write_privileges, status)
VALUES (UUID(), '#osu', 'General discussion.', 0, 1, 1),
       (UUID(), '#lobby', 'General multiplayer lobby chat.', 0, 1, 1),
       (UUID(), '#announce', 'Announcements from the server.', 0, 1 << 30, 1),
       (UUID(), '#help', 'Help and support.', 0, 1, 1),

       (UUID(), '#staff', 'General discussion for staff members.',
        (1 << 7 | 1 << 9 | 1 << 13 | 1 << 30),
        (1 << 7 | 1 << 9 | 1 << 13 | 1 << 30), 1),
       (UUID(), '#devlog', 'Development updates and logs.', 0, 1 << 30, 1),

       (UUID(), '#plus', 'Supporter exclusive channel.', 1 << 2, 1 << 2, 1),
       (UUID(), '#supporter', 'Supporter exclusive channel.', 1 << 2, 1 << 2, 1),
       (UUID(), '#premium', 'Premium supporter exclusive channel.', 1 << 3, 1 << 3, 1);

INSERT INTO users (id, username, safe_username, email, password_bcrypt, registration_time, latest_activity, country,
                   privileges)
VALUES (1, 'BanchoBot', 'banchobot', 'bot@osupe.ru', '__a_dummy_bcrypt_password__',
        NOW(), NOW(), 0, 0);

ALTER TABLE users auto_increment = 3;