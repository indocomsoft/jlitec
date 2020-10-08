class Main {
  Void main() {
    Gcd gcd;
    Div div;
    Int dividend;
    Int divisor;
    gcd = new Gcd();
    println("The GCD of 20 and 30 is");
    println(gcd.gcd(20, 30));
    println("Now, let's try Division!");
    println("Enter dividend:");
    readln(dividend);
    println("Enter divisor:");
    readln(divisor);
    if (divisor == 0) {
      println("Invalid division by zero");
      return;
    } else {
      while(false) {}
    }
    div = new Div();
    div.dividend = dividend;
    div.divisor = divisor;
    div.perform();
    println("Quotient = ");
    println(div.quotient);
    println("Remainder = ");
    println(div.remainder);
  }
}

class Gcd {
  Int gcd(Int a, Int b) {
    while (a != b) {
      if (a > b) {
        a = a - b;
      } else {
        b = b - a;
      }
    }
    return a;
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
      while (false) {}
    }
      this.quotient = 0;
    while (dividend >= this.divisor) {
      dividend = dividend - this.divisor;
      this.quotient = this.quotient + 1;
    }
    this.remainder = dividend;
  }
}
