CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   email TEXT UNIQUE NOT NULL,
   password TEXT NOT NULL,
   role TEXT NOT NULL,
   balance INT DEFAULT 0,
   blocked BOOLEAN DEFAULT FALSE
);

CREATE TABLE tasks (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   title TEXT NOT NULL,
   description TEXT,
   reward INT NOT NULL,
   status TEXT NOT NULL,
   customer_id UUID NOT NULL REFERENCES users(id),
   executor_id UUID REFERENCES users(id)
);

CREATE TABLE bids (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
  executor_id UUID NOT NULL REFERENCES users(id),
  status TEXT NOT NULL
);

CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  from_user UUID NOT NULL REFERENCES users(id),
  to_user UUID NOT NULL REFERENCES users(id),
  amount INT NOT NULL,
  created_at TIMESTAMP DEFAULT now()
);