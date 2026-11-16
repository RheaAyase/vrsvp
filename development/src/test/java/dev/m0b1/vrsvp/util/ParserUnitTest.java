package dev.m0b1.vrsvp.util;

import dev.m0b1.vrsvp.TestBase;
import dev.m0b1.vrsvp.button.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ParserUnitTest extends TestBase {

	@BeforeEach
	void beforeEach() {
		when(message.getContentRaw()).thenReturn("non-embed content");
		when(message.getEmbeds()).thenReturn(List.of(messageEmbed));
	}

	@Test
	void utilityClass() throws NoSuchMethodException {
		assertUtilityClass(Parser.class);
	}

	@Test
	void readsContentRawIfMessageEmbedsNotFound() {
		when(message.getEmbeds()).thenReturn(null);
		assertEquals("non-embed content", Parser.readMessageDescription(message));
	}

	@Test
	void readsContentRawIfMessageEmbedsEmpty() {
		when(message.getEmbeds()).thenReturn(Collections.emptyList());
		assertEquals("non-embed content", Parser.readMessageDescription(message));
	}

	@Test
	void readNullMessageDescriptionDefaultsToEmptyString() {
		when(messageEmbed.getDescription()).thenReturn(null);
		assertEquals("", Parser.readMessageDescription(message));
	}

	@Test
	void readMessageDescription() {
		when(messageEmbed.getDescription()).thenReturn("Testing");
		assertEquals("Testing", Parser.readMessageDescription(message));
	}

	@Test
	void isSlot() {
		List.of("> #", "> #1", "> #2").forEach(line -> assertTrue(Parser.isSlot(line)));
		List.of("> ", " #", "").forEach(line -> assertFalse(Parser.isSlot(line)));
	}

	@Test
	void isSignup() {
		List.of("> ", "> @Testing (healer)").forEach(line -> assertTrue(Parser.isSignup(line)));
		List.of("> #1", ">", "@Testing", "").forEach(line -> assertFalse(Parser.isSignup(line)));
	}

	@Test
	void findSignupsInSlotWithSlotLines() {

		var input = """
			> #1, <t:5:t>, @Test1 (cc), @Test2
			> #2, <t:6:t>""";

		assertEquals(List.of("@Test1 (cc)", "@Test2"), Parser.findSignupsInSlot(input, 0));
		assertEquals(List.of(), Parser.findSignupsInSlot(input, 1));
		assertNull(Parser.findSignupsInSlot(input, 2));
	}

	@Test
	void findSignupsInSlotWithoutSlotLines() {

		var input = """
			**Signups**
			> @Test1 (cc)
			> @Test2 (healer)""";

		assertEquals(List.of("@Test1 (cc)", "@Test2 (healer)"), Parser.findSignupsInSlot(input, 0));
		assertEquals(List.of(), Parser.findSignupsInSlot("**Signups**", 0));
		assertNull(Parser.findSignupsInSlot(input, 1));
	}

	@Test
	void findRolesInSignup() {
		assertEquals(List.of(RoleEnum.HEALER), Parser.findRolesInSignup("@Testing (healer)"));
		assertEquals(List.of(RoleEnum.BARD, RoleEnum.RANGED_DPS), Parser.findRolesInSignup("@Testing (bard/ranged dps) "));
		assertEquals(List.of(RoleEnum.CC), Parser.findRolesInSignup("(note) @Testing (cc/unknown)"));
		assertEquals(List.of(), Parser.findRolesInSignup("@Testing"));
	}

	@Test
	void countSlotsInText() {

		var input = """
			Header
			> Ignored
			#1 Still ignored
			> #1

			> #2""";

		assertEquals(2, Parser.countSlotsInText(input));
	}

	@Test
	void splitSlotTextIsEditable() {

		var result = Parser.splitSlotText("a, b, c");
		result.remove(1);

		assertEquals(2, result.size());
		assertEquals("a", result.get(0));
		assertEquals("c", result.get(1));
	}

	@Test
	void findRsvpLimitPerSlotInTextWithoutMatch() {
		assertNull(Parser.findRsvpLimitPerSlotInText(null));
		assertNull(Parser.findRsvpLimitPerSlotInText(" Maximum number of people: 2"));
	}

	@Test
	void findRsvpLimitPerSlotInText() {

		var input = """
			   Maximum number of people that can RSVP: 2 (until further notice)
			More lines""";

		assertEquals(2, Parser.findRsvpLimitPerSlotInText(input));
	}

	@Test
	void findRsvpLimitPerSlotInTextOnlyTakesFirstResult() {

		var input = """
			Maximum number of people that can RSVP: 2
			Maximum number of people that can RSVP: 1""";

		assertEquals(2, Parser.findRsvpLimitPerSlotInText(input));
	}

}
