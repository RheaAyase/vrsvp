package dev.m0b1.vrsvp.command;

import dev.m0b1.vrsvp.TestBase;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlashCommandUiUnitTest extends TestBase {

	private final SlashCommandData command = SlashCommandUi.create();

	@Test
	void utilityClass() throws NoSuchMethodException {
		assertUtilityClass(SlashCommandUi.class);
	}

	@Test
	void formed() {
		assertEquals(Command.Type.SLASH, command.getType());
		assertEquals(SlashCommandUi.INVOCATION, command.getName());
	}

	@Test
	void onlyUsableByAdmins() {
		assertEquals(DefaultMemberPermissions.DISABLED, command.getDefaultPermissions());
	}

	@Test
	void inputCountIsExpected() {
		var options = command.getOptions();
		assertEquals(2, options.size());
	}

	@Test
	void inputsAreMarkedRequiredAsNeeded() {
		var options = command.getOptions();
		assertFalse(options.get(0).isRequired()); // Start
		assertFalse(options.get(1).isRequired()); // RSVP limit per slot
	}

	@Test
	void inputsAreNumbers() {
		assertTrue(command.getOptions().stream().allMatch(option -> option.getType() == OptionType.INTEGER));
	}

	@Test
	void inputsUseLogicalOrder() {
		var options = command.getOptions();
		assertEquals(SlashCommandEnum.START.getId(), options.get(0).getName());
		assertEquals(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getId(), options.get(1).getName());
	}

	@Test
	void inputsHaveMaximumValues() {
		var options = command.getOptions();
		assertEquals(SlashCommandEnum.START.getMaximum(), options.get(0).getMaxValue());
		assertEquals(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getMaximum(), options.get(1).getMaxValue());
	}

	@Test
	void inputsHaveMinimumValues() {
		var options = command.getOptions();
		assertEquals(SlashCommandEnum.START.getMinimum(), options.get(0).getMinValue());
		assertEquals(SlashCommandEnum.RSVP_LIMIT_PER_SLOT.getMinimum(), options.get(1).getMinValue());
	}

}
