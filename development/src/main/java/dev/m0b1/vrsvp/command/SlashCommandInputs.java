package dev.m0b1.vrsvp.command;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class SlashCommandInputs {

    private Integer startTimestamp;
	private Integer rsvpLimitPerSlot;

}
