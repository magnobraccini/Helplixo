package helplixo.web.edicao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import helplixo.web.edicao.EdicaoServico.EntradaRecusada;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Regras da senha de edição, com um relógio que o teste adianta. Senha inventada só para o teste. */
class EdicaoServicoTest {

    static final String SENHA = "senha-do-teste-unitario";

    /** Relógio que o teste pode adiantar. */
    static class RelogioManual extends Clock {
        Instant agora = Instant.parse("2026-11-26T13:00:00Z");
        void adiantar(Duration d) { agora = agora.plus(d); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    final RelogioManual relogio = new RelogioManual();

    EdicaoServico servico(String senha) {
        return new EdicaoServico(senha, 8, relogio);
    }

    @Test
    void semAVariavelOModoEdicaoFicaDesligado() {
        EdicaoServico s = servico("");
        assertThat(s.configurado()).isFalse();
        assertThatThrownBy(() -> s.entrar("qualquer", "pc1"))
                .isInstanceOf(EntradaRecusada.class)
                .extracting(e -> ((EntradaRecusada) e).motivo()).isEqualTo(EntradaRecusada.Motivo.DESLIGADO);
    }

    @Test
    void senhaCertaDaUmaChaveQueValeOitoHoras() {
        EdicaoServico s = servico(SENHA);
        EdicaoServico.Entrada e = s.entrar(SENHA, "pc1");
        assertThat(e.chave()).hasSizeGreaterThan(30);
        assertThat(e.validaAte()).isEqualTo(relogio.agora.plus(Duration.ofHours(8)));
        s.exigir(e.chave()); // não lança
        relogio.adiantar(Duration.ofHours(8));
        assertThat(s.validaAte(e.chave())).isEmpty();
        assertThatThrownBy(() -> s.exigir(e.chave())).isInstanceOf(EdicaoServico.SemEdicaoException.class);
    }

    @Test
    void senhaErradaNaoDaChave() {
        EdicaoServico s = servico(SENHA);
        assertThatThrownBy(() -> s.entrar("senha-do-teste", "pc1"))
                .isInstanceOf(EntradaRecusada.class).hasMessageContaining("Senha incorreta");
        assertThatThrownBy(() -> s.exigir("chave-inventada")).isInstanceOf(EdicaoServico.SemEdicaoException.class);
    }

    @Test
    void cincoErrosSeguidosFazemEsperarUmMinuto() {
        EdicaoServico s = servico(SENHA);
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> s.entrar("errada", "pc1")).isInstanceOf(EntradaRecusada.class);
        }
        assertThatThrownBy(() -> s.entrar("errada", "pc1")).hasMessageContaining("espere 1 minuto");
        // nem a senha certa entra durante a espera
        assertThatThrownBy(() -> s.entrar(SENHA, "pc1"))
                .extracting(e -> ((EntradaRecusada) e).motivo()).isEqualTo(EntradaRecusada.Motivo.ESPERAR);
        // outro computador não é afetado
        assertThat(s.entrar(SENHA, "pc2").chave()).isNotBlank();
        relogio.adiantar(Duration.ofSeconds(61));
        assertThat(s.entrar(SENHA, "pc1").chave()).isNotBlank();
    }

    @Test
    void acertarZeraOsErros() {
        EdicaoServico s = servico(SENHA);
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> s.entrar("errada", "pc1")).isInstanceOf(EntradaRecusada.class);
        }
        s.entrar(SENHA, "pc1");
        // depois de acertar, mais 4 erros ainda não bloqueiam
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> s.entrar("errada", "pc1")).hasMessageContaining("Senha incorreta. Confira");
        }
    }

    @Test
    void sairInvalidaAChave() {
        EdicaoServico s = servico(SENHA);
        String chave = s.entrar(SENHA, "pc1").chave();
        s.sair(chave);
        assertThat(s.validaAte(chave)).isEmpty();
    }
}
