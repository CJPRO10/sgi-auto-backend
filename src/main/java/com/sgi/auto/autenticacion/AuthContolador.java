package com.sgi.auto.autenticacion;

import com.sgi.auto.compartido.ApiRespuesta;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.sgi.auto.autenticacion.dto.LoginSolicitudDTO;
import com.sgi.auto.autenticacion.dto.TokenRespuestaDTO;
@RestController
@RequestMapping("/api/autenticacion")
@RequiredArgsConstructor

public class AuthContolador {
    private final AuthServicio authServicio;

    @PostMapping("/ingresar")
    public ResponseEntity<ApiRespuesta<TokenRespuestaDTO>> ingresar(
            @Valid @RequestBody LoginSolicitudDTO solicitud) {

        TokenRespuestaDTO respuesta = authServicio.ingresar(solicitud);
        return ResponseEntity.ok(ApiRespuesta.exitoso(respuesta, "Ingreso exitoso"));
    }
    @GetMapping("/../../salud")  // /api/salud — público
    public ResponseEntity<ApiRespuesta<String>> salud() {
        return ResponseEntity.ok(ApiRespuesta.exitoso("SGI-AUTO operativo"));
    }
}
