-- Enums para PostgreSQL (opcional, pero recomendado)
CREATE TYPE rol_usuario AS ENUM ('USER', 'LIBRARIAN', 'ADMIN');
CREATE TYPE estado_usuario AS ENUM ('ACTIVO', 'BLOQUEADO');
CREATE TYPE estado_libro AS ENUM ('DISPONIBLE', 'PRESTADO', 'RESERVADO', 'PERDIDO');
CREATE TYPE estado_prestamo AS ENUM ('ACTIVO', 'DEVUELTO', 'ATRASADO');
CREATE TYPE estado_multa AS ENUM ('PENDIENTE', 'PAGADO');

-- Tabla de Usuarios
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    contrasena VARCHAR(100) NOT NULL,
    dni VARCHAR(8) UNIQUE,
    telefono VARCHAR(15),
    rol rol_usuario DEFAULT 'USER',
    estado estado_usuario DEFAULT 'ACTIVO'
);

-- Tabla de Libros
CREATE TABLE libros (
    isbn VARCHAR(13) PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    autor VARCHAR(100) NOT NULL,
    categoria VARCHAR(50),
    año_pub INT CHECK (año_pub BETWEEN 1900 AND EXTRACT(YEAR FROM CURRENT_DATE)),
    editorial VARCHAR(100),
    copias_disponibles INT DEFAULT 1,
    estado estado_libro DEFAULT 'DISPONIBLE'
);

-- Tabla de Préstamos
CREATE TABLE prestamos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id),
    libro_isbn VARCHAR(13) REFERENCES libros(isbn),
    fecha_prestamo DATE DEFAULT CURRENT_DATE,
    fecha_devolucion DATE NOT NULL,
    fecha_entrega_real DATE,
    estado estado_prestamo DEFAULT 'ACTIVO'
);

-- Tabla de Multas
CREATE TABLE multas (
    id BIGSERIAL PRIMARY KEY,
    prestamo_id BIGINT REFERENCES prestamos(id),
    monto DECIMAL(10, 2) NOT NULL,
    estado estado_multa DEFAULT 'PENDIENTE'
);

-- Índices para performance
CREATE INDEX idx_libros_autor ON libros(autor);
CREATE INDEX idx_prestamos_usuario_id ON prestamos(usuario_id);