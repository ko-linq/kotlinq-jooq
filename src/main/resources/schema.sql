CREATE TABLE employee(
    id NUMBER(7) PRIMARY KEY NOT NULL,
    username VARCHAR(120) NOT NULL ,
    age NUMBER(3) NOT NULL ,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE tracked_time(
    id NUMBER(7) PRIMARY KEY NOT NULL,
    user_id NUMBER(7) NOT NULL ,
    date DATE NOT NULL ,
    minutes INTEGER NOT NULL ,

    CONSTRAINT fk_time_user FOREIGN KEY (user_id) REFERENCES employee(id)
);
