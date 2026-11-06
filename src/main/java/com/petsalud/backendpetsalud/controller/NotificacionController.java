package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Notificacion;
import com.petsalud.backendpetsalud.service.NotificacionService;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@AllArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<Notificacion> enviarNotificacion(@PathVariable Long usuarioId, @RequestBody Notificacion notificacion) {
        Notificacion nuevaNotificacion = notificacionService.enviarNotificacion(usuarioId, notificacion);
        return ResponseEntity.ok(nuevaNotificacion);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.notificacion(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> obtenerNotificacionPorId(@PathVariable Long id) {
        return notificacionService.obtenerNotificacionPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.usuario(authentication, #usuarioId)")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacion>> listarNotificacionesPorUsuario(@PathVariable Long usuarioId) {
        List<Notificacion> notificaciones = notificacionService.listarNotificacionesPorUsuario(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.usuario(authentication, #usuarioId)")
    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<List<Notificacion>> listarNotificacionesNoLeidas(@PathVariable Long usuarioId) {
        List<Notificacion> notificaciones = notificacionService.listarNotificacionesNoLeidas(usuarioId);
        return ResponseEntity.ok(notificaciones);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.notificacion(authentication, #id)")
    @PutMapping("/{id}/leida")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNotificacion(@PathVariable Long id) {
        notificacionService.eliminarNotificacion(id);
        return ResponseEntity.noContent().build();
    }
}
