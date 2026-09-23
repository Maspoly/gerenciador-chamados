-- =========================================================
-- V19 - Estrutura para Reservas de Áreas Comuns
-- Adiciona tabelas de áreas comuns, reservas e histórico
-- =========================================================

-- 1. Tabela de Áreas Comuns (Churrasqueira, Salão de Festas, etc.)
CREATE TABLE areas_comuns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(500),
    ativa BOOLEAN NOT NULL DEFAULT true
);

-- 2. Tabela Principal de Reservas
CREATE TABLE reservas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_comum_id UUID NOT NULL,
    morador_id UUID NOT NULL,
    data_hora_inicio TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    data_hora_fim TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    motivo_negativa VARCHAR(500),
    data_criacao TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservas_area_comum FOREIGN KEY (area_comum_id) REFERENCES areas_comuns(id),
    CONSTRAINT fk_reservas_morador FOREIGN KEY (morador_id) REFERENCES moradores(id),
    CONSTRAINT chk_reservas_horario CHECK (data_hora_fim > data_hora_inicio),
    CONSTRAINT chk_reservas_status CHECK (status IN ('SOLICITADA', 'APROVADA', 'NEGADA', 'CANCELADA'))
);

-- 3. Tabela de Histórico de Modificações da Reserva
CREATE TABLE historico_reservas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reserva_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    status_anterior VARCHAR(50),
    status_novo VARCHAR(50) NOT NULL,
    motivo VARCHAR(500),
    data_alteracao TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historico_reservas_reserva FOREIGN KEY (reserva_id) REFERENCES reservas(id) ON DELETE CASCADE,
    CONSTRAINT fk_historico_reservas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- 4. Índices para performance em buscas e verificações de disponibilidade
CREATE INDEX idx_reservas_area_comum_id ON reservas(area_comum_id);
CREATE INDEX idx_reservas_morador_id ON reservas(morador_id);
CREATE INDEX idx_reservas_status ON reservas(status);
CREATE INDEX idx_reservas_periodo ON reservas(area_comum_id, data_hora_inicio, data_hora_fim);
CREATE INDEX idx_historico_reservas_reserva_id ON historico_reservas(reserva_id);