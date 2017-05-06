#
# Create database user hokan_ng and grant (too much) privileges
#

CREATE USER 'hokan_ng'@'localhost' IDENTIFIED BY 'hokan_ng';
GRANT ALL PRIVILEGES ON  *.* to 'hokan_ng'@'localhost' WITH GRANT OPTION;

CREATE USER 'hokan_ng'@'%' IDENTIFIED BY 'hokan_ng';
GRANT ALL PRIVILEGES ON  *.* to 'hokan_ng'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;
