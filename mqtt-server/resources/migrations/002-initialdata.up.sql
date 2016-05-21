INSERT INTO stores (name,store_type,version,uri,id) VALUES ('gj1','geojson','1','simple.geojson','3dc5afc9-393b-444c-8581-582e2c2d98a3');
INSERT INTO stores (name,store_type,version,uri,id) VALUES ('gj2','geojson','1','feature.geojson','3e6c072e-8e62-41be-8d1a-7a3116df9c16');
INSERT INTO stores (name,store_type,version,uri,id) VALUES ('gpkg1','gpkg','1','http://www.geopackage.org/data/haiti-vectors-split.gpkg','f6dcc750-1349-46b9-a324-0223764d46d1');
INSERT INTO stores (name,store_type,version,uri,id) VALUES ('gpkg2','gpkg','1','https://portal.opengeospatial.org/files/63156','fad33ae1-f529-4c79-affc-befc37c104ae');
INSERT INTO devices (name,identifier) VALUES ('iphone','afsasdfasdfasdfsf');
INSERT INTO devices (name,identifier) VALUES ('android',';ljljlkjljljljlkj');
INSERT INTO forms (name) VALUES ('one');
INSERT INTO forms (name) VALUES ('two');
INSERT INTO form_def (type,label,required,form_id) VALUES ('string','Father',true,(SELECT id FROM forms WHERE name = 'one'));
INSERT INTO form_def (type,label,required,form_id) VALUES ('number','Michael',true,(SELECT id FROM forms WHERE name = 'two'));
INSERT INTO form_def (type,label,required,form_id) VALUES ('string','Tito',true,(SELECT id FROM forms WHERE name = 'two'));