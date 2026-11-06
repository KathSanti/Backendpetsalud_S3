package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.service.MascotaService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/mascotas")
@AllArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    // Un cliente solo puede registrar mascotas a su propio nombre.
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.usuario(authentication, #propietarioId)")
    @PostMapping("/propietario/{propietarioId}")
    public ResponseEntity<Mascota> registrarMascota(@PathVariable Long propietarioId, @RequestBody Mascota mascota) {
        Mascota nuevaMascota = mascotaService.registrarMascota(mascota, propietarioId);
        return ResponseEntity.ok(nuevaMascota);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.mascota(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<Mascota> obtenerMascotaPorId(@PathVariable Long id) {
        return mascotaService.obtenerMascotaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.mascota(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<Mascota> actualizarMascota(@PathVariable Long id, @RequestBody Mascota mascota) {
        Mascota mascotaActualizada = mascotaService.actualizarMascota(id, mascota);
        return ResponseEntity.ok(mascotaActualizada);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMascota(@PathVariable Long id) {
        mascotaService.eliminarMascota(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.usuario(authentication, #propietarioId)")
    @GetMapping("/propietario/{propietarioId}")
    public ResponseEntity<List<Mascota>> listarMascotasPorPropietario(@PathVariable Long propietarioId) {
        List<Mascota> mascotas = mascotaService.listarMascotasPorPropietario(propietarioId);
        return ResponseEntity.ok(mascotas);
    }
}
