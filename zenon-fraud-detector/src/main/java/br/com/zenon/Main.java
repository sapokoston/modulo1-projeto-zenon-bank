
package br.com.zenon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Main {

    void main() {

        var t1 = new Transaction(
                1,
                TransactionType.PAYMENT,
                new BigDecimal("9838.64"),
                new TransactionCustomer(
                        "C1231006815",
                        new BigDecimal("170136.0"),
                        new BigDecimal("160296.36")
                ),
                new TransactionCustomer(
                        "M1979787155",
                        new BigDecimal("0.0"),
                        new BigDecimal("0.0")
                ),
                false,
                false
        );

        var t2 = new Transaction(
                743,
                TransactionType.CASH_OUT,
                new BigDecimal("850002.52"),
                new TransactionCustomer(
                        "C1280323807",
                        new BigDecimal("850002.52"),
                        new BigDecimal("0.0")
                ),
                new TransactionCustomer(
                        "C873221189",
                        new BigDecimal("6510099.11"),
                        new BigDecimal("7360101.63")
                ),
                true,
                false
        );

        System.out.println(t1);
        System.out.println(t2);
        System.out.println("----------------------");

        var transactionIngestor = new TransactionIngestor();

        List<Transaction> transactions =
                transactionIngestor.readNew("data/PS_20174392719_1491204439457_log.csv");

        IO.println(transactions.size());

        transactions.stream()
                .limit(10)
                .forEach(System.out::println);

        System.out.println("------------Tratamento de erros-------------------");

        List<Transaction> transactionsBadData =
                transactionIngestor.readNew("data/paysim_with_bad_data.csv");

        IO.println(transactions.size());
        System.out.println("Quantidade de linhas: " + transactionsBadData.size());
        transactionsBadData.forEach(System.out::println);

        System.out.println("------------FRAUDE-------------------");

        var fraudAnalyzer = new FraudAnalyzer(transactions);

        // Apenas transações onde isFraud == true
        long fraudCount = fraudAnalyzer.countFraud();
        IO.println("Total de fraudes " + fraudCount);

        // Imprima as 3 maiores fraudes
        List<Transaction> highestFrauds = fraudAnalyzer.findHighestValuesFrauds(3);

        IO.println("Top 3 fraudes de maior valor:");
        highestFrauds.stream()
                .map(Transaction::amount)
                .forEach(IO::println);

        // Obter os nomes dos clientes de origem das fraudes
        // e gerar uma lista sem repetição com os 5 maiores suspeitos
        List<String> suspiciousClients = fraudAnalyzer.findTopSuspiciousClients(5);

        IO.println("Top 5 clientes suspeitos:");
        suspiciousClients.forEach(IO::println);

        // Calcular o prejuízo total causado pelas fraudes
        BigDecimal totalFraudLoss = fraudAnalyzer.calculateTotalFraudeLoss();

        IO.println("Prejuízo total causado pelas fraudes: " + totalFraudLoss);

        // Contar quantas fraudes ocorreram por tipo de transação
        Map<TransactionType, Long> fraudsByType = fraudAnalyzer.countFraudsByType();

        IO.println("Fraudes por tipo:");
        fraudsByType.forEach((type, count) ->
                IO.println(type + " - " + count)
        );

        IO.println("Fraudes por tipo: " + fraudsByType);

        // ================= BENCHMARK =================

        System.out.println("------------benchmark-------------------");

        // Criando repositório List
        TransactionRepository transactionRepository =
                new TransactionListRepository(transactions);

        // Teste 1 - Transação inexistente
        String notFoundOriginName = "c12345";

        transactionRepository.findByOriginName(notFoundOriginName)
                .ifPresentOrElse(
                        IO::println,
                        () -> IO.println("Transação não encontrada: " + notFoundOriginName)
                );

        // Teste 2 - Transação existente
        String existingOriginName = "C1868032458";

        // ================= LIST =================

        long startTimeList = System.nanoTime();

        var resultList = transactionRepository.findByOriginName(existingOriginName);

        long endTimeList = System.nanoTime();

        resultList.ifPresentOrElse(
                IO::println,
                () -> IO.println("Transação não encontrada: " + existingOriginName)
        );

        IO.println("Tempo de busca para List (ms): "
                + (endTimeList - startTimeList) / 1_000_000.0);

        // ================= MAP =================

        transactionRepository = new TransactionMapRepository(transactions);

        long startTimeMap = System.nanoTime();

        var resultMap = transactionRepository.findByOriginName(existingOriginName);

        long endTimeMap = System.nanoTime();

        resultMap.ifPresentOrElse(
                IO::println,
                () -> IO.println("Transação não encontrada: " + existingOriginName)
        );

        IO.println("Tempo de busca para Map (ms): "
                + (endTimeMap - startTimeMap) / 1_000_000.0);

        System.out.println("------------FIM BENCHMARK-------------------");
    }
}
