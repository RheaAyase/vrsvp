package dev.m0b1.vrsvp.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SlashCommandEnum {

	RSVP_LIMIT_PER_SLOT(
		"limit",
		null,
		0,
		"Maximum number of people that can RSVP for a single slot",
		false
	),

	START(
		"time",
		2_051_244_000L, // 2035-01-01
		0,
		"Timestamp from https://hammertime.cyou/ like '1684043839', defaults to now",
		false
	);

	private final String id;
	private final Long maximum;
	private final long minimum;
	private final String description;
	private final boolean required;

}
