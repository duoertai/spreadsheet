import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assumptions:
 * 1. values could be either integer or a formula in string
 * 2. if a cell is not set, it's treated as 0 (as in real spreadsheet)
 * 3. for cell id format, there could be any number of '0' before row number, for example, A0001 is the same as A1 (as in real spreadsheet)
 * 4. column name treat upper case and lower case letter as the same, for example, a1 is the same as A1 (as in real spreadsheet)
 * 5. use in memory map for to store cells
 * 6. number of rows is within the range of java int type
 * 7. formula must start with '=' (as in real spreadsheet)
 * 8. formula could have empty space within the string
 * 9. support + - * / ( ), integer division should truncate toward zero
 * 10. constants/numbers in formula could have 0 prevailing number(s), for example, a1 + 0001 is valid (as in real spreadsheet)
 * 11. assuming formula is always valid
 */
public class SpreadSheetImpl implements SpreadSheet{
    private final Map<String, Integer> numbers;
    private final Map<String, String> formula;

    private final Pattern cellPattern = Pattern.compile("^[a-zA-Z]+0*[1-9]+$");
    private final Pattern numberPattern = Pattern.compile("^0*[1-9]+$");

    public SpreadSheetImpl() {
        numbers = new HashMap<>();
        formula = new HashMap<>();
    }

    public boolean validateCellId(String cellId) {
        Matcher matcher = cellPattern.matcher(cellId);
        return matcher.find();
    }

    public boolean isNumber(String str) {
        return numberPattern.matcher(str).find();
    }

    public int calculate(String s) {
        return calculate(s, 0, s.length() - 1);
    }

    public int calculate(String s, int start, int end) {
        char op = '+';
        String operand = "";
        int num = 0;
        Stack<Integer> stack = new Stack<>();

        for(int i = start; i <= end; i++) {
            char c = s.charAt(i);

            if((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')){
                operand = operand + c;
            } else if(c == '(') {
                int j = i;
                int diff = 0;
                while(j <= end) {
                    if(s.charAt(j) == '(')
                        diff++;
                    else if(s.charAt(j) == ')')
                        diff--;

                    if(diff == 0)
                        break;
                    j++;
                }
                operand = String.valueOf(calculate(s, i + 1, j - 1));
                i = j;
            }

            if(i == end || (c != ' ' && !((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) && c != ')' && c != '(')){
                if(isNumber(operand)) {
                    num = Integer.parseInt(operand);
                } else if(validateCellId(operand)) {
                    num = getCellValue(operand);
                }
                if(op == '+') {
                    stack.push(num);
                } else if(op == '-') {
                    stack.push(-num);
                } else if(op == '*') {
                    stack.push(stack.pop() * num);
                } else if(op == '/') {
                    stack.push(stack.pop() / num);
                }

                op = c;
                operand = "";
            }
        }

        int res = 0;
        while(!stack.isEmpty())
            res += stack.pop();
        return res;
    }

    @Override
    public void setCellValue(String cellId, Object value) {
        if(!validateCellId(cellId)) {
            throw new IllegalArgumentException("Cell id is not valid");
        }

        String id = cellId.toUpperCase();

        if(value instanceof Integer) {
            numbers.put(id, (Integer) value);
        } else if(value instanceof String) {
            formula.put(id, ((String) value).substring(1).trim());
        }
    }

    @Override
    public int getCellValue(String cellId) {
        if(!validateCellId(cellId)) {
            throw new IllegalArgumentException("Cell id is not valid");
        }

        String id = cellId.toUpperCase();

        if(formula.containsKey(id)) {
            return calculate(formula.get(id));
        } else {
            return numbers.containsKey(id) ? numbers.get(id) : 0;
        }
    }


    public static void main(String[] arg) {
        SpreadSheetImpl spreadSheet = new SpreadSheetImpl();
        spreadSheet.setCellValue("a1", 13);
        System.out.println("A1 = 13 <--> " + spreadSheet.getCellValue("a1"));

        spreadSheet.setCellValue("A2", 14);
        System.out.println("A2 = 14 <--> " + spreadSheet.getCellValue("A2"));

        spreadSheet.setCellValue("a3", "=A1+ A2");
        System.out.println("A3 = 13 + 14 <--> " + spreadSheet.getCellValue("a3"));

        spreadSheet.setCellValue("A4", "=A1+ A2 + a3");
        System.out.println("A4 = 13 + 14 + 27 <--> " + spreadSheet.getCellValue("a4"));

        spreadSheet.setCellValue("A5", "=(A1+ A2 )* a3");
        System.out.println("A5 = (13 + 14) * 27 <--> " + spreadSheet.getCellValue("A5"));

        spreadSheet.setCellValue("A6", "=(A18+ A2 )/ 14");
        System.out.println("A6 = (0 + 14) / 14 <--> " + spreadSheet.getCellValue("A6"));
    }
}
