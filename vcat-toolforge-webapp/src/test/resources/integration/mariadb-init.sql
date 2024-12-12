create table wiki (
    dbname varchar(255),
    family varchar(255),
    is_closed bit,
    lang varchar(255),
    name varchar(255),
    url varchar(255)
);

insert into wiki (dbname, family, is_closed, lang, name, url) values
    ('dewiki', 'wikipedia', 0, 'de', 'Wikipedia', 'https://de.wikipedia.org'),
    ('enwiki', 'wikipedia', 0, 'en', 'Wikipedia', 'https://en.wikipedia.org');
