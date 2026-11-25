package dev.m0b1.vrsvp.button;

import dev.m0b1.vrsvp.TestBase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ButtonUiUnitTest extends TestBase {

	@Test
	void utilityClass() throws NoSuchMethodException {
		assertUtilityClass(ButtonUi.class);
	}

	@Test
	void buildRsvpActionPrompts() {

		var buttons = ButtonUi.buildRsvpActionPrompts();

		assertEquals(2, buttons.size());
		assertButtonInformation(buttons.get(0), ButtonStyle.PRIMARY, ButtonEnum.RSVP, ButtonUi.EMOJI_RSVP);
		assertButtonInformation(buttons.get(1), ButtonStyle.SECONDARY, ButtonEnum.EDIT, ButtonUi.EMOJI_EDIT);
	}

	@Test
	void buildEditActionPrompts() {

		var buttons = ButtonUi.buildEditActionPrompts();

		assertEquals(2, buttons.size());

		assertButtonInformation(buttons.get(0), ButtonStyle.PRIMARY, ButtonEnum.EDIT_EVENT_DESCRIPTION, null);
		assertButtonInformation(buttons.get(1), ButtonStyle.DANGER, ButtonEnum.EDIT_EVENT_ACTIVE, null);
	}

	@Test
	void buildIndexedButtonActionRowsPartitionCorrectly() {

		var buttonRows = ButtonUi.buildIndexedButtonActionRows(ButtonEnum.RSVP.getId(), 11);

		assertEquals(3, buttonRows.size());
		assertEquals(5, buttonRows.get(0).getButtons().size());
		assertEquals(5, buttonRows.get(1).getButtons().size());
		assertEquals(1, buttonRows.get(2).getButtons().size());
	}

	@Test
	void buildIndexedButtonActionRowsFormButtonsCorrectly() {

		var buttonRows = ButtonUi.buildIndexedButtonActionRows(ButtonEnum.RSVP.getId(), 2);
		var buttons = buttonRows.getFirst().getButtons();

		assertButtonInformation(buttons.get(0), ButtonStyle.PRIMARY, "rsvp:0", "#1", null);
		assertButtonInformation(buttons.get(1), ButtonStyle.PRIMARY, "rsvp:1", "#2", null);
	}

	@Test
	void buildRoleSelectActionRow() {

		var menu = ButtonUi.buildRoleSelectActionRow(2, null).getComponents().getFirst().asStringSelectMenu();

		assertEquals("rsvp-role:2", menu.getCustomId());
		assertEquals(ButtonEnum.RSVP_ROLE.getLabel(), menu.getPlaceholder());
		assertEquals(RoleEnum.values().length, menu.getOptions().size());
		assertEquals("healer", menu.getOptions().getFirst().getValue());
		assertEquals("lp-dispel sage", menu.getOptions().getLast().getLabel());
		assertEquals(1, menu.getMinValues());
		assertEquals(RoleEnum.values().length, menu.getMaxValues());
		assertTrue(menu.getOptions().stream().noneMatch(SelectOption::isDefault));
	}

	@Test
	void buildRoleSelectActionRowPreselectsCurrentRoles() {

		var menu = ButtonUi.buildRoleSelectActionRow(0, List.of(RoleEnum.BARD, RoleEnum.CC))
			.getComponents()
			.getFirst()
			.asStringSelectMenu();

		var defaults = menu.getOptions().stream().filter(SelectOption::isDefault).map(SelectOption::getValue).toList();
		assertEquals(List.of("bard", "cc"), defaults);
	}

	@Test
	void buildRsvpActionRowsForMultipleSlotsUsesSlotButtons() {

		var actionRows = ButtonUi.buildRsvpActionRows(2, null);

		assertEquals(1, actionRows.size());
		assertEquals(2, actionRows.getFirst().getButtons().size());
	}

	@Test
	void buildRsvpActionRowsForSingleSlotUsesRoleDropdown() {

		var actionRows = ButtonUi.buildRsvpActionRows(0, null);

		assertEquals(1, actionRows.size());
		assertEquals("rsvp-role:0", actionRows.getFirst().getComponents().getFirst().asStringSelectMenu().getCustomId());
	}

	@Test
	void buildRsvpActionRowsForSingleSlotWhenSignedUpAddsRemoveButton() {

		var actionRows = ButtonUi.buildRsvpActionRows(1, List.of());

		assertEquals(2, actionRows.size());
		assertButtonInformation(actionRows.get(1).getButtons().getFirst(), ButtonStyle.DANGER, "rsvp:0", "Remove RSVP", null);
	}

	@Test
	void editEmbedDescriptionWithoutSlotLinesAppendsSignupOnItsOwnLine() {
		var result = editRsvp("**Signups**", "@Testing (healer)");
		assertEquals("**Signups**\n> @Testing (healer)", result);
	}

	@Test
	void editEmbedDescriptionWithoutSlotLinesReplacesExistingSignup() {
		var result = editRsvp("**Signups**\n> @Testing (bard)\n> @Test1 (cc)", "@Testing (healer)");
		assertEquals("**Signups**\n> @Test1 (cc)\n> @Testing (healer)", result);
	}

	@Test
	void editEmbedDescriptionWithoutSlotLinesRemovesSignup() {
		var result = editRsvp("**Signups**\n> @Test1 (cc)\n> @Testing (bard)", null);
		assertEquals("**Signups**\n> @Test1 (cc)", result);
	}

	@Test
	void buildSignup() {
		assertEquals("@Testing (ranged dps)", ButtonUi.buildSignup("@Testing", List.of(RoleEnum.RANGED_DPS)));
		assertEquals("@Testing (healer/bard)", ButtonUi.buildSignup("@Testing", List.of(RoleEnum.HEALER, RoleEnum.BARD)));
	}

	@Test
	void editEmbedDescriptionFromRsvpAddsSignup() {
		var result = editRsvp("> #1, <t:5:t>", "@Testing (healer)");
		assertEquals("> #1, <t:5:t>, @Testing (healer)", result);
	}

	@Test
	void editEmbedDescriptionFromRsvpWithExistingSignupReplacesIt() {
		var result = editRsvp("> #1, <t:5:t>, @Testing (bard), @Test1 (cc)", "@Testing (healer)");
		assertEquals("> #1, <t:5:t>, @Test1 (cc), @Testing (healer)", result);
	}

	@Test
	void editEmbedDescriptionFromRsvpWithExistingMentionRemovesIt() {
		var result = editRsvp("> #1, <t:5:t>, @Testing", null);
		assertEquals("> #1, <t:5:t>", result);
	}

	@Test
	void editEmbedDescriptionFromRsvpWithExistingMentionsAddsToEnd() {
		var result = editRsvp("> #1, <t:5:t>, @Test1, @Test2", "@Testing");
		assertEquals("> #1, <t:5:t>, @Test1, @Test2, @Testing", result);
	}

	@Test
	void editEmbedDescriptionFromRsvpWithWithExistingMentionBetweenOtherMentionsRemovesIt() {
		var result = editRsvp("> #1, <t:5:t>, @Test1, @Testing (dancer), @Test2", null);
		assertEquals("> #1, <t:5:t>, @Test1, @Test2", result);
	}

	@Test
	void editEmbedDescriptionFromRsvpForMoreThanOneFieldIsAllowed() {

		var embed = new EmbedBuilder().setDescription("> #1, <t:0:F>, @Testing\n> #2, <t:1:F>").build();
		when(message.getEmbeds()).thenReturn(List.of(embed));

		var editedDescription = ButtonUi.editRsvpInSlot(message, "@Testing", "@Testing", 1);
		var slots = editedDescription.split("\n");

		assertEquals("> #1, <t:0:F>, @Testing", slots[0]);
		assertEquals("> #2, <t:1:F>, @Testing", slots[1]);
	}

	// Test utility method(s)

	private void assertButtonInformation(Button button, ButtonStyle buttonStyle, ButtonEnum buttonEnum, Emoji emoji) {
		assertButtonInformation(button, buttonStyle, buttonEnum.getId(), buttonEnum.getLabel(), emoji);
	}

	private void assertButtonInformation(
		Button button,
		ButtonStyle buttonStyle,
		String id,
		String label,
		Emoji emoji
	) {

		assertEquals(buttonStyle, button.getStyle());
		assertEquals(label, button.getLabel());
		assertEquals(emoji, button.getEmoji());

		if (ButtonStyle.LINK.equals(buttonStyle)) {
			assertNull(button.getCustomId());
			assertEquals(id, button.getUrl());
		} else {
			assertEquals(id, button.getCustomId());
			assertNull(button.getUrl());
		}
	}

	private String editRsvp(String slotValue, String signup) {

		var embed = new EmbedBuilder().setDescription(slotValue).build();
		when(message.getEmbeds()).thenReturn(List.of(embed));

		return ButtonUi.editRsvpInSlot(message, "@Testing", signup, 0);
	}

}
