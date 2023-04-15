CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  first_name TEXT,
  last_name TEXT,
  nickname TEXT,
  telegram_id TEXT,
  created_at DATE
);

CREATE TABLE courses (
  id INTEGER PRIMARY KEY,
  name TEXT,
  display_name TEXT,
  author TEXT,
  source TEXT,
  description TEXT,
  source_url TEXT,
  created_at DATE
);

CREATE TABLE course_elements (
  id INTEGER PRIMARY KEY,
  course_id INTEGER,
  parent_id INTEGER,
  name TEXT,
  display_name TEXT,
  created_at DATE,
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (parent) REFERENCES course_elements(id)
);

CREATE TABLE user_course (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  course_id INTEGER,
  status TEXT,
  created_at DATE
);

CREATE TABLE user_progress (
  id INTEGER PRIMARY KEY,
  user_id INTEGER NOT NULL,
  course_id INTEGER,
  element_id INTEGER NOT NULL,
  user_course_id INTEGER NOT NULL,
  element_type TEXT NOT NULL,
  status TEXT NOT NULL,
  created_at DATE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (user_course_id) REFERENCES user_course(id),
  UNIQUE(user_id, element_id, element_type)
);

CREATE TABLE documents (
  id INTEGER PRIMARY KEY,
  name TEXT,
  display_name TEXT,
  tg_file_id TEXT,
  url TEXT,
  course_id INTEGER,
  course_element_id INTEGER,
  type TEXT,
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (course_element_id) REFERENCES course_elements(id)
);

CREATE TABLE IF NOT EXISTS chats(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id INTEGER,
    type TEXT
);
