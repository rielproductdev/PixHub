package com.riel.pixhub.validation;

/**
 * Validador algorítmico de CPF e CNPJ.
 *
 * Usa o algoritmo de módulo 11 para verificar se os dígitos verificadores
 * (os 2 últimos dígitos) são matematicamente corretos.
 *
 * Isso NÃO verifica se o CPF/CNPJ pertence a alguém real —
 * para isso precisaria consultar a Receita Federal (serviço pago).
 * Para nosso sistema, validar o formato já é suficiente para rejeitar
 * entradas inválidas e demonstrar conhecimento técnico.
 *
 * Por que métodos static?
 * O validator não tem estado — só faz cálculos. Chamamos direto:
 *   DocumentValidator.isValidCpf("12345678901")
 *
 * Por que uma classe separada e não dentro do Service?
 * Separação de responsabilidades (SRP — Single Responsibility Principle):
 * - O Service cuida de regras de NEGÓCIO (limite de chaves, duplicidade)
 * - O Validator cuida de regras de FORMATO (CPF válido, CNPJ válido)
 * - Se amanhã precisarmos validar CPF em outro Service, reutilizamos
 */
public class DocumentValidator {

    /**
     * Valida um CPF usando o algoritmo de módulo 11.
     *
     * Passos do algoritmo:
     * 1. Verifica se tem exatamente 11 dígitos
     * 2. Rejeita CPFs com todos os dígitos iguais (111.111.111-11)
     * 3. Calcula o 1º dígito verificador a partir dos 9 primeiros dígitos
     * 4. Calcula o 2º dígito verificador a partir dos 10 primeiros dígitos
     * 5. Compara os dígitos calculados com os informados
     *
     * Exemplo: CPF 529.982.247-25
     * - Dígitos: 5 2 9 9 8 2 2 4 7 [2] [5]
     * - Os 9 primeiros (529982247) geram os 2 últimos (25)
     * - Se alguém inventar "52998224799", o algoritmo detecta que é falso
     *
     * @param cpf String com exatamente 11 dígitos numéricos (sem formatação)
     * @return true se o CPF é válido, false caso contrário
     */
    public static boolean isValidCpf(String cpf) {
        // Passo 1: deve ter exatamente 11 dígitos
        if (cpf == null || cpf.length() != 11 || !cpf.matches("\\d+")) {
            return false;
        }

        // Passo 2: rejeita CPFs com todos os dígitos iguais
        // 000.000.000-00, 111.111.111-11, etc. são matematicamente "válidos"
        // pelo módulo 11, mas não existem de verdade — são armadilhas comuns
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }

        // Passo 3: calcula o 1º dígito verificador
        // Multiplica cada um dos 9 primeiros dígitos por pesos decrescentes (10, 9, 8...)
        // e soma tudo
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        // Aplica módulo 11: o resto da divisão por 11
        int firstDigit = 11 - (sum % 11);
        // Se o resultado for 10 ou 11, o dígito verificador é 0
        if (firstDigit >= 10) {
            firstDigit = 0;
        }

        // Compara com o 10º dígito do CPF informado
        if (Character.getNumericValue(cpf.charAt(9)) != firstDigit) {
            return false;
        }

        // Passo 4: calcula o 2º dígito verificador
        // Mesma lógica, mas agora com os 10 primeiros dígitos e pesos (11, 10, 9...)
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int secondDigit = 11 - (sum % 11);
        if (secondDigit >= 10) {
            secondDigit = 0;
        }

        // Compara com o 11º dígito do CPF informado
        return Character.getNumericValue(cpf.charAt(10)) == secondDigit;
    }

    /**
     * Valida um CNPJ usando o algoritmo de módulo 11.
     *
     * Mesma lógica do CPF, mas com 14 dígitos e pesos diferentes.
     *
     * CNPJ tem formato: XX.XXX.XXX/XXXX-XX (14 dígitos)
     * Os 12 primeiros são o número, os 2 últimos são verificadores.
     *
     * Pesos do CNPJ (diferente do CPF):
     * 1º dígito: 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2
     * 2º dígito: 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2
     *
     * @param cnpj String com exatamente 14 dígitos numéricos (sem formatação)
     * @return true se o CNPJ é válido, false caso contrário
     */
    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14 || !cnpj.matches("\\d+")) {
            return false;
        }

        // Rejeita CNPJs com todos os dígitos iguais
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }

        // Pesos para o 1º dígito verificador do CNPJ
        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights1[i];
        }
        int firstDigit = 11 - (sum % 11);
        if (firstDigit >= 10) {
            firstDigit = 0;
        }

        if (Character.getNumericValue(cnpj.charAt(12)) != firstDigit) {
            return false;
        }

        // Pesos para o 2º dígito verificador do CNPJ
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights2[i];
        }
        int secondDigit = 11 - (sum % 11);
        if (secondDigit >= 10) {
            secondDigit = 0;
        }

        return Character.getNumericValue(cnpj.charAt(13)) == secondDigit;
    }

    /**
     * Valida um documento (CPF ou CNPJ) automaticamente pelo tamanho.
     *
     * Método de conveniência — o Service chama este método sem precisar
     * verificar o tamanho antes. Ele decide qual validação usar:
     * - 11 dígitos → valida como CPF
     * - 14 dígitos → valida como CNPJ
     * - Outro tamanho → inválido
     *
     * @param document String com dígitos numéricos
     * @return true se o documento é válido, false caso contrário
     */
    public static boolean isValidDocument(String document) {
        if (document == null) {
            return false;
        }
        return switch (document.length()) {
            case 11 -> isValidCpf(document);
            case 14 -> isValidCnpj(document);
            default -> false;
        };
    }
}
