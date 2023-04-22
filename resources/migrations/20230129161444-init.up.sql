CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  first_name VARCHAR,
  last_name VARCHAR,
  username VARCHAR,
  telegram_id INTEGER,
  created_at DATE
);
--;;
CREATE TABLE courses (
  id SERIAL PRIMARY KEY,
  name VARCHAR,
  display_name VARCHAR,
  author VARCHAR,
  source VARCHAR,
  description VARCHAR,
  source_url VARCHAR,
  created_at DATE
);
--;;
CREATE TABLE course_elements (
  id SERIAL PRIMARY KEY,
  course_id INTEGER,
  parent_id INTEGER,
  name VARCHAR,
  display_name VARCHAR,
  created_at DATE,
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (parent_id) REFERENCES course_elements(id)
);
--;;
CREATE TABLE user_course (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  course_id INTEGER,
  status VARCHAR,
  created_at DATE
);
--;;
CREATE TABLE user_progress (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  course_id INTEGER,
  element_id INTEGER NOT NULL,
  user_course_id INTEGER NOT NULL,
  element_type VARCHAR NOT NULL,
  status VARCHAR NOT NULL,
  created_at DATE,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (user_course_id) REFERENCES user_course(id),
  UNIQUE(user_id, element_id, element_type)
);
--;;
CREATE TABLE documents (
  id SERIAL PRIMARY KEY,
  name VARCHAR,
  display_name VARCHAR,
  tg_file_id VARCHAR,
  url VARCHAR,
  course_id INTEGER,
  course_element_id INTEGER,
  type VARCHAR,
  FOREIGN KEY (course_id) REFERENCES courses(id),
  FOREIGN KEY (course_element_id) REFERENCES course_elements(id)
);
--;;
CREATE TABLE IF NOT EXISTS chats(
    id SERIAL PRIMARY KEY,
    telegram_id INTEGER,
    type VARCHAR
);
--;;