package br.com.magnobraccini.helplixo.core;

/**
 * Não cria dados de demonstração. Cada instalação deve receber seus próprios processos,
 * contatos e ramais fora do repositório de código.
 */
public final class DemoDataSeeder {
    public void seed(ApplicationContext context) {
        if (!context.getConfiguration().isDemoDataSeeded()) {
            context.getConfiguration().setDemoDataSeeded(true);
            context.getConfigManager().save(context.getConfiguration());
        }
    }
}