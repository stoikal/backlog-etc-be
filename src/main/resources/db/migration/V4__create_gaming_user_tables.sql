CREATE TABLE gaming.lists (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id),
    title TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

CREATE TABLE gaming.list_items (
    list_id UUID NOT NULL REFERENCES gaming.lists(id) ON UPDATE CASCADE ON DELETE CASCADE,
    game_id INTEGER NOT NULL REFERENCES gaming.games(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (list_id, game_id)
);

CREATE TABLE gaming.games_comments (
    game_id INTEGER NOT NULL REFERENCES gaming.games(id),
    user_id UUID NOT NULL REFERENCES identity.users(id),
    comment TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (game_id, user_id)
);

CREATE TABLE gaming.games_statuses (
    user_id UUID NOT NULL REFERENCES identity.users(id),
    game_id INTEGER NOT NULL REFERENCES gaming.games(id),
    status TEXT NOT NULL DEFAULT 'todo',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    CONSTRAINT games_statuses_status_check CHECK (status = ANY (ARRAY['todo', 'playing', 'finished'])),
    PRIMARY KEY (user_id, game_id)
);