CREATE TABLE `posts` (
  `post_id` varchar(8) NOT NULL,
  `comments` mediumtext,
  `picture` mediumblob,
  PRIMARY KEY (`post_id`)
)