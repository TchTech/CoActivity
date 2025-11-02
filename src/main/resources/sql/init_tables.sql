CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    userName VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE rooms (
    id SERIAL PRIMARY KEY,
    destription TEXT,
    interestType INTEGER,
    geoposition TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    createdBy INTEGER,
    maxColaborators INTEGER

);
CREATE TABLE roomsAdmins (
    roomId INTEGER,
    adminId INTEGER,
    FOREIGN KEY (roomId) REFERENCES rooms (id),
    FOREIGN KEY (adminId) REFERENCES users (id)
);
CREATE TABLE roomsColaborators (
    roomId INTEGER,
    colaboratorId INTEGER,
    FOREIGN KEY (roomId) REFERENCES rooms (id),
    FOREIGN KEY (colaboratorId) REFERENCES users (id)
);
CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    createdBy INTEGER REFERENCES users (id) ON DELETE CASCADE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    roomID INTEGER REFERENCES rooms (id) ON DELETE CASCADE,
    imageID INTEGER
);
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    createdBy INTEGER REFERENCES users (id) ON DELETE CASCADE,
    content TEXT,
    room INTEGER REFERENCES rooms (id) ON DELETE CASCADE,
    image VARCHAR(255)
);
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    post INTEGER REFERENCES posts (id) ON DELETE CASCADE,
    createdBy INTEGER REFERENCES users (id) ON DELETE CASCADE,
    content TEXT
);

CREATE TABLE likesAndDislikes (
    id SERIAL PRIMARY KEY,
    type BIT,
    post INTEGER REFERENCES posts (id) ON DELETE CASCADE,
    comment INTEGER REFERENCES comments (id) ON DELETE CASCADE,
    createdBy INTEGER REFERENCES users (id) ON DELETE CASCADE

);

CREATE TABLE interestCategory (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    description TEXT
);
CREATE TABLE interests (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    category INTEGER REFERENCES interestCategory (id)
);

CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    userId INTEGER REFERENCES users (id) on DELETE CASCADE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    content TEXT,
    title VARCHAR(255) NOT NULL,
    isRead BIT
);