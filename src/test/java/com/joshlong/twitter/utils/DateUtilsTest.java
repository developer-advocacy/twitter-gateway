package com.joshlong.twitter.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class DateUtilsTest {

	@Test
	void parseIsoDateTime() {
		var date = DateUtils.readIsoDateTime("2022-02-13T13:35:09.840Z");
		log.info(date.toString());
	}

}