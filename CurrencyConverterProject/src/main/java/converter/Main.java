package converter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Scanner;

public class Main {

    public static HashMap<String, BigDecimal> currencyMap = new HashMap<>();

    public static void main(String[] args) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            String url = "https://www.cbar.az/currencies/" + currentDate() + ".xml";
            Document document = builder.parse(url);
            Element valType = (Element) document.getElementsByTagName("ValType").item(1);
            NodeList nodeList = valType.getElementsByTagName("Valute");


            for (int i = 0; i < nodeList.getLength(); i++) {
                EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
                EntityTransaction entityTransaction = em.getTransaction();
                entityTransaction.begin();

                Element element = (Element) nodeList.item(i);
                Currency currency = Currency.builder()
                        .code(element.getAttribute("Code"))
                        .nominal(Integer.parseInt(element.getElementsByTagName("Nominal").item(0).getTextContent()))
                        .value(new BigDecimal(element.getElementsByTagName("Value").item(0).getTextContent()))
                        .build();

                /**
                 * Putting the real AZN equivalent of currencies into the map
                 * For example 100 (which is nominal) KRW is 0.1531 AZN,
                 * That is why, I divide this value to nominal to get real value
                 * which is 1 KRW == 0.001531 AZN
                 */
                currencyMap.put(currency.getCode(),
                        currency.getValue().divide(BigDecimal.valueOf(currency.getNominal())));
                em.merge(currency);
                em.getTransaction().commit();
                em.close();
            }

            /**
             * Putting AZN into the map as well as in database
             */
            EntityManager em = JPAUtil.getEntityManagerFactory().createEntityManager();
            EntityTransaction entityTransaction = em.getTransaction();
            entityTransaction.begin();

            Currency currency = Currency.builder().code("AZN").nominal(1).value(BigDecimal.valueOf(1)).build();
            currencyMap.put(currency.getCode(), currency.getValue());

            em.merge(currency);
            em.getTransaction().commit();
            em.close();
            JPAUtil.shutdown();

            /**
             * Getting the inputs from client
             */
            Currency currencyFrom = getCurrencyFrom();
            Currency currencyTo = getCurrencyTo();
            Double value = getValue();

            /**
             * Showing the output to the client
             */
            System.out.println("*".repeat(50));
            System.out.println(value + " " + currencyFrom.getCode() + " is " +
                    currencyMap.get(currencyFrom.getCode()).multiply(BigDecimal.valueOf(value))
                            .divide(currencyMap.get(currencyTo.getCode()), 6, RoundingMode.HALF_UP) +
                    " " + currencyTo.getCode());


        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method is for getting current date in "dd.MM.yyyy" format
     * @return
     */
    private static String currentDate() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return now.format(formatter);
    }


    /**
     * This method is for getting currency to convert from
     * @return
     */
    public static Currency getCurrencyFrom() {
        Currency currencyFrom = new Currency();
        OUT:
        while (true) {
            System.out.println("Please, enter the currency that you want to convert from " +
                    "(in a form of AZN, USD, EUR and etc.)");
            Scanner scanner = new Scanner(System.in);
            String in_curr = scanner.nextLine();

            int count = 0;
            for (String c : currencyMap.keySet()) {
                count++;
                if (c.equals(in_curr)) {
                    count--;
                    currencyFrom = currencyFrom.toBuilder().code(c).value(currencyMap.get(c)).build();
                    break OUT;
                } else if (count == currencyMap.size()) {
                    System.out.println("No such currency is found, please try again.");
                    continue;
                }
            }
        }
        return currencyFrom;
    }

    /**
     * This method is for getting currency to convert to
     * @return
     */
    public static Currency getCurrencyTo() {
        Currency currencyTo = new Currency();
        OUT:
        while (true) {
            System.out.println("Please, enter the currency that you want to convert to " +
                    "(in a form of AZN, USD, EUR and etc.)");
            Scanner scanner = new Scanner(System.in);
            String out_curr = scanner.nextLine();

            int count = 0;
            for (String c : currencyMap.keySet()) {
                count++;
                if (c.equals(out_curr)) {
                    count--;
                    currencyTo = currencyTo.toBuilder().code(c).value(currencyMap.get(c)).build();
                    break OUT;
                } else if (count == currencyMap.size()) {
                    System.out.println("No such currency is found, please try again.");
                    continue;
                }
            }
        }
        return currencyTo;
    }

    /**
     * This method is for getting converting value as double
     * @return
     */
    public static double getValue() {
        System.out.println("Please, enter the value that you want to convert ");
        while (true) {
            Scanner scanner = new Scanner(System.in);
            String data = scanner.nextLine();
            if (data.matches("[0-9]+[.]?[0-9]+")) {
                return Double.parseDouble(data);
            } else {
                System.out.println("Please enter value in correct format (as double number)");
            }
        }
    }



}
