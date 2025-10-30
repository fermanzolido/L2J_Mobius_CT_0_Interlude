CREATE TABLE IF NOT EXISTS `bbs_memo` (
  `memo_id` int(11) NOT NULL AUTO_INCREMENT,
  `char_id` int(11) NOT NULL,
  `memo` text NOT NULL,
  PRIMARY KEY (`memo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
