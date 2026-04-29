CREATE TABLE beatmaps
(
    id              INT          NOT NULL PRIMARY KEY,
    mode            INT          NOT NULL,
    md5             VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    version         VARCHAR(128) NOT NULL,
    submission_date datetime     NOT NULL,
    last_updated    datetime     NOT NULL,
    playcount       BIGINT       NOT NULL,
    passcount       BIGINT       NOT NULL,
    total_length    INT          NOT NULL,
    drain_length    INT          NOT NULL,
    count_normal    INT          NOT NULL,
    count_slider    INT          NOT NULL,
    count_spinner   INT          NOT NULL,
    max_combo       INT          NOT NULL,
    bpm             REAL         NOT NULL,
    cs              REAL         NOT NULL,
    ar              REAL         NOT NULL,
    od              REAL         NOT NULL,
    hp              REAL         NOT NULL,
    star_rating     REAL         NOT NULL,
    beatmapset_id   INT          NOT NULL
);

CREATE TABLE beatmapsets
(
    id                INT           NOT NULL PRIMARY KEY,
    title             VARCHAR(128)  NULL,
    title_unicode     VARCHAR(128)  NULL,
    artist            VARCHAR(128)  NULL,
    artist_unicode    VARCHAR(128)  NULL,
    source            VARCHAR(128)  NULL,
    source_unicode    VARCHAR(128)  NULL,
    creator           VARCHAR(128)  NULL,
    tags              VARCHAR(1024) NULL,
    submission_status VARCHAR(32)   NOT NULL,
    has_video         BIT(1)        NOT NULL,
    has_storyboard    BIT(1)        NOT NULL,
    submission_date   datetime      NOT NULL,
    approved_date     datetime      NULL,
    last_updated      datetime      NOT NULL,
    total_playcount   BIGINT        NOT NULL,
    language_id       INT           NOT NULL,
    genre_id          INT           NOT NULL
);

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

CREATE TABLE scores
(
    id                 BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id            INT                   NOT NULL,
    online_checksum    VARCHAR(32)           NOT NULL,
    beatmap_id         INT                   NOT NULL,
    score              BIGINT                NOT NULL,
    performance_points DOUBLE                NOT NULL,
    accuracy           DOUBLE                NOT NULL,
    highest_combo      INT                   NOT NULL,
    full_combo         BIT(1)                NOT NULL,
    mods               INT                   NOT NULL,
    num_300s           INT                   NOT NULL,
    num_100s           INT                   NOT NULL,
    num_50s            INT                   NOT NULL,
    num_misses         INT                   NOT NULL,
    num_gekis          INT                   NOT NULL,
    num_katus          INT                   NOT NULL,
    grade              VARCHAR(2)            NOT NULL,
    submission_status  VARCHAR(255)          NOT NULL,
    mode               INT                   NOT NULL,
    passed             BIT(1)                NOT NULL,
    time_elapsed       INT                   NOT NULL,
    created_at         datetime              NOT NULL,
    updated_at         datetime              NOT NULL,
    CONSTRAINT fk_scores_on_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_scores_on_beatmap FOREIGN KEY (beatmap_id) REFERENCES beatmaps (id)
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
    latest_performance_point_awarded  datetime           NOT NULL DEFAULT 0,
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

CREATE INDEX beatmaps_md5_idx ON beatmaps (md5);
CREATE INDEX idx_users_safe_name ON users (safe_username);
CREATE INDEX idx_stats_user_mode ON stats (user_id, mode);
CREATE INDEX idx_hw_md5_lookup ON hardware_logs (adapters_md5, uninstall_md5, disk_signature_md5);
CREATE INDEX idx_messages_unread ON messages (target_id, deleted_at, read_at);
CREATE INDEX idx_messages_sender_stats ON messages (sender_id, created_at);
CREATE INDEX score_user_mode_status_pp_idx ON scores (user_id, mode, submission_status, performance_points DESC);
CREATE INDEX beatmap_mode_status_idx ON scores (beatmap_id);
CREATE INDEX beatmap_status_idx ON scores (submission_status);