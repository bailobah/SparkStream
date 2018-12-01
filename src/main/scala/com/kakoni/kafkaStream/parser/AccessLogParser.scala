package com.kakoni.kafkaStream.parser

import org.apache.log4j.Logger

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

object AccessLogParser {

  val log = Logger.getLogger(getClass.getName)
  private val regex: Regex = "([^-]*)\\s+-\\s+(\\S+)\\s+\\[(\\d{2}\\/[a-zA-Z]{3}\\/\\d{4}:\\d{2}:\\d{2}:\\d{2}\\s+-\\d{4})\\]\\s+\"(.+)\"\\s+(\\d{1,}\\.\\d{3})\\s+(\\d+)\\s+\"([^\"]+)\"\\s+Agent\\[\"([^\"]+)\"\\]\\s+(-|\\d.\\d{3,})\\s+(\\S+)\\s+(\\d{1,}).*".r


  def parser(str: String): AccessLogRecord ={

     regex.findFirstMatchIn(str) match {
       case Some(matcher) => buildAccessLogRecord(matcher = matcher)
       case None => AccessLogRecord()
     }

  }
  private def buildAccessLogRecord(matcher: Match) = {
    AccessLogRecord(
      matcher.group(1),
      matcher.group(2),
      matcher.group(3),
      matcher.group(4),
      matcher.group(5),
      matcher.group(6),
      matcher.group(7),
      matcher.group(8),
      matcher.group(9))
  }

}
