CREATE TABLE channels
(
    id               VARCHAR(36)  NOT NULL PRIMARY KEY DEFAULT (UUID()),
    name             VARCHAR(96)  NOT NULL,
    description      VARCHAR(256) NOT NULL,
    read_privileges  INT          NOT NULL DEFAULT 0,
    write_privileges INT          NOT NULL DEFAULT 0,
    status           BIT(1)       NOT NULL DEFAULT 0,
    CONSTRAINT uc_channels_name UNIQUE (name)
);

CREATE TABLE users
(
    id                INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    username          VARCHAR(32) NOT NULL,
    safe_username     VARCHAR(32) NOT NULL,
    email             VARCHAR(64) NOT NULL,
    password_bcrypt   VARCHAR(60) NOT NULL,
    registration_time datetime    NOT NULL,
    latest_activity   datetime    NOT NULL,
    country           SMALLINT    NOT NULL,
    silence_end       datetime    NULL,
    privileges        INT         NOT NULL DEFAULT 1,
    CONSTRAINT uc_users_username UNIQUE (username),
    CONSTRAINT uc_users_safe_username UNIQUE (safe_username),
    CONSTRAINT uc_users_email UNIQUE (email)
);

CREATE TABLE stats
(
    id                                INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id                           INT                NULL,
    mode                              INT                NOT NULL DEFAULT 0,
    ranked_score                      BIGINT             NOT NULL DEFAULT 0,
    total_score                       BIGINT             NOT NULL DEFAULT 0,
    play_count                        INT                NOT NULL DEFAULT 0,
    replays_watched                   INT                NOT NULL DEFAULT 0,
    total_hits                        INT                NOT NULL DEFAULT 0,
    level                             INT                NOT NULL DEFAULT 0,
    average_accuracy                  DOUBLE             NOT NULL DEFAULT 0.0,
    performance_points                INT                NOT NULL DEFAULT 0,
    play_time                         INT                NOT NULL DEFAULT 0,
    xh_count                          INT                NOT NULL DEFAULT 0,
    x_count                           INT                NOT NULL DEFAULT 0,
    sh_count                          INT                NOT NULL DEFAULT 0,
    s_count                           INT                NOT NULL DEFAULT 0,
    a_count                           INT                NOT NULL DEFAULT 0,
    b_count                           INT                NOT NULL DEFAULT 0,
    c_count                           INT                NOT NULL DEFAULT 0,
    d_count                           INT                NOT NULL DEFAULT 0,
    max_combo                         INT                NOT NULL DEFAULT 0,
    latest_performance_point_awarded  INT                NOT NULL DEFAULT 0,
    CONSTRAINT fk_stats_on_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE relationships
(
    id          INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    follower_id INT                NOT NULL,
    friend_id   INT                NOT NULL,
    CONSTRAINT fk_relationships_on_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_relationships_on_friend FOREIGN KEY (friend_id) REFERENCES users (id)
);

CREATE TABLE messages
(
    id               INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    sender_id        INT                NOT NULL,
    sender_name      VARCHAR(32)        NOT NULL,
    target_id        INT                NULL,
    target_channel   VARCHAR(64)        NULL,
    content          VARCHAR(2048)      NOT NULL,
    read_at          DATETIME           NULL,
    created_at       DATETIME           NOT NULL,
    deleted_at       DATETIME           NULL
);

CREATE TABLE hardware_logs
(
    id                  INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id             INT                NULL,
    adapters_md5        VARCHAR(32)        NOT NULL,
    uninstall_md5       VARCHAR(32)        NOT NULL,
    disk_signature_md5  VARCHAR(32)        NOT NULL,
    ocurrencies         INT                NOT NULL DEFAULT 0,
    activated           BIGINT             NOT NULL,
    last_used           DATETIME           NOT NULL,
    CONSTRAINT fk_hardware_logs_on_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE matches
(
    id         BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    name       VARCHAR(128)          NOT NULL,
    `private`  BIT(1)                NOT NULL,
    start_time datetime              NOT NULL,
    end_time   datetime              NULL
);

CREATE TABLE match_games
(
    id            INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    match_id      BIGINT             NOT NULL,
    beatmap_id    INT                NOT NULL,
    mode          INT                NOT NULL DEFAULT 0,
    mods          INT                NOT NULL,
    win_condition INT                NOT NULL,
    team_type     INT                NOT NULL,
    start_time    datetime           NOT NULL,
    end_time      datetime           NULL,
    CONSTRAINT fk_match_games_on_match FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE match_events
(
    id         INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    match_id   BIGINT             NOT NULL,
    game_id    INT                NULL,
    user_id    INT                NOT NULL,
    event_type VARCHAR(32)        NOT NULL,
    timestamp  datetime           NOT NULL,
    CONSTRAINT fk_match_events_on_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_events_on_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_users_safe_name ON users (safe_username);
CREATE INDEX idx_stats_user_mode ON stats (user_id, mode);
CREATE INDEX idx_hw_md5_lookup ON hardware_logs (adapters_md5, uninstall_md5, disk_signature_md5);
CREATE INDEX idx_messages_unread ON messages (target_id, deleted_at, read_at);
CREATE INDEX idx_messages_sender_stats ON messages (sender_id, created_at);