package com.libromagico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Email(message = "Email inválido")
    @NotBlank(message = "El email es obligatorio")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;

    @Pattern(regexp = "^[0-9]{8}$", message = "DNI debe tener 8 dígitos")
    @Column(unique = true)
    private String dni;

    @Pattern(regexp = "^\\+[0-9]{10,15}$", message = "Teléfono debe incluir código de país")
    private String telefono;

    @Enumerated(EnumType.STRING)
    private RolUsuario rol = RolUsuario.USER;

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;
}

public enum RolUsuario {
    USER, LIBRARIAN, ADMIN
}

public enum EstadoUsuario {
    ACTIVO, BLOQUEADO
}