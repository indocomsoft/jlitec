class Main {
  Void main() {
    Div div;
    Int dividend;
    Int divisor;
    dividend = 100;
    divisor = 30;
    div = new Div();
    div.dividend = dividend;
    div.divisor = divisor;
    div.perform();
    println("Dividend = ");
    println(dividend); // should print 100
    println(div.dividend); // should print 100
    println("Divisor = ");
    println(divisor); // should print 30
    println(div.divisor); // should print 30
    println("Quotient = ");
    println(div.quotient); // should print 3
    println("Remainder = ");
    println(div.remainder); // should print 10
  }
}

class Div {
  Int dividend;
  Int divisor;

  Int quotient;
  Int remainder;

  Void perform() {
    Int dividend;
    dividend = this.dividend;
    if (divisor == 0) {
      this.quotient = -1;
      this.remainder = -1;
      return;
    } else {
      while (false) { }
    }
    this.quotient = 0;
    while (dividend >= this.divisor) {
      dividend = dividend - this.divisor;
      this.quotient = this.quotient + 1;
    }
    this.remainder = dividend;
  }
}
