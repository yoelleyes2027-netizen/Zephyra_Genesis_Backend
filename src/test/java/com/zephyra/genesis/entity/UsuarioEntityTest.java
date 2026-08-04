package com.zephyra.genesis.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UsuarioEntityTest {

    @Test
    void shouldStoreRoleAndProfilePhoto() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setCedula(123456);
        usuario.setPassword("secret123");
        usuario.setRol(ROL.ADMIN);

        byte[] foto = new byte[]{1, 2, 3, 4};
        usuario.setFotoPerfil(foto);

        assertEquals(123456, usuario.getCedula());
        assertEquals("secret123", usuario.getPassword());
        assertEquals(ROL.ADMIN, usuario.getRol());
        assertArrayEquals(foto, usuario.getFotoPerfil());
    }
}
