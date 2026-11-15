package net.stackanvil.deep.rivet;

import net.lenni0451.rivet.input.keyboard.Key;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLScancode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivetInputMapperTest {

    @Test
    void mapsKnownScancodes() {
        assertEquals(Key.A, RivetInputMapper.fromScancode(SDLScancode.SDL_SCANCODE_A).orElseThrow());
        assertEquals(Key.KP_ENTER, RivetInputMapper.fromScancode(SDLScancode.SDL_SCANCODE_KP_ENTER).orElseThrow());
    }

    @Test
    void ignoresUnknownScancodes() {
        assertTrue(RivetInputMapper.fromScancode(SDLScancode.SDL_SCANCODE_UNKNOWN).isEmpty());
    }

    @Test
    void mapsRivetKeysBackToScancodes() {
        assertEquals(SDLScancode.SDL_SCANCODE_RGUI, RivetInputMapper.toScancode(Key.RIGHT_SUPER).orElseThrow());
        assertEquals(SDLScancode.SDL_SCANCODE_SPACE, RivetInputMapper.toScancode(Key.SPACE).orElseThrow());
    }
}
