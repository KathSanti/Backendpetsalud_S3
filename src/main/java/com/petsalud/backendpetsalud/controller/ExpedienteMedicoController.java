package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;

import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/expedientes")
@AllArgsConstructor
public class ExpedienteMedicoController {

    private final ExpedienteMedicoService expedienteMedicoService;

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @PostMapping
    public ResponseEntity<ExpedienteMedico> agregarRegistro(
            @RequestParam Long mascotaId,
            @RequestParam Long veterinarioId,
            @RequestBody ExpedienteMedico registro) {
        ExpedienteMedico nuevoRegistro = expedienteMedicoService.agregarRegistro(mascotaId, veterinarioId, registro);
        return ResponseEntity.ok(nuevoRegistro);
    }

    // Datos clínicos: el cliente solo ve los expedientes de sus mascotas.
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.expediente(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteMedico> obtenerRegistroPorId(@PathVariable Long id) {
        return expedienteMedicoService.obtenerRegistroPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.mascota(authentication, #mascotaId)")
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<List<ExpedienteMedico>> listarRegistrosPorMascota(@PathVariable Long mascotaId) {
        List<ExpedienteMedico> registros = expedienteMedicoService.listarRegistrosPorMascota(mascotaId);
        return ResponseEntity.ok(registros);
    }

    @PreAuthorize("hasRole('GERENTE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarRegistro(@PathVariable Long id) {
        expedienteMedicoService.eliminarRegistro(id);
        return ResponseEntity.noContent().build();
    }
}
