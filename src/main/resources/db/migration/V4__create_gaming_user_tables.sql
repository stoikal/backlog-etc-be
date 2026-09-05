CREATE TABLE gaming.lists (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_user UUID NOT NULL REFERENCES identity.users(id),
    title TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

CREATE TABLE gaming.list_items (
    id_list UUID NOT NULL REFERENCES gaming.lists(id) ON UPDATE CASCADE ON DELETE CASCADE,
    id_game INTEGER NOT NULL REFERENCES gaming.games(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (id_list, id_game)
);

CREATE TABLE gaming.games_comments (
    id_game INTEGER NOT NULL REFERENCES gaming.games(id),
    id_user UUID NOT NULL REFERENCES identity.users(id),
    comment TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (id_game, id_user)
);

CREATE TABLE gaming.games_statuses (
    id_user UUID NOT NULL REFERENCES identity.users(id),
    id_game INTEGER NOT NULL REFERENCES gaming.games(id),
    status TEXT NOT NULL DEFAULT 'todo',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    CONSTRAINT games_statuses_status_check CHECK (status = ANY (ARRAY['todo', 'playing', 'finished'])),
    PRIMARY KEY (id_user, id_game)
);