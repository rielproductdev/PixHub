package com.riel.pixhub.enums;

/**
 * Tipos de conta bancária suportados pelo sistema.
 *
 * No PIX real, tanto contas correntes quanto poupança podem
 * ter chaves PIX e fazer/receber transferências.
 *
 * Por que usar enum e não String?
 * Se fosse String, alguém poderia salvar "checking", "CHECKING", "Corrente"...
 * Com enum, o compilador garante que só valores válidos são usados.
 * Se adicionarmos SALARY (conta salário) no futuro, o compilador mostra
 * todos os switch/case que precisam tratar o novo tipo.
 */
public enum AccountType {

    /** Conta corrente — tipo mais comum para operações PIX */
    CHECKING,

    /** Conta poupança — também pode ter chaves PIX */
    SAVINGS
}
