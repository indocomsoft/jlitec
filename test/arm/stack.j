class A {
  Void main() {
    Div div;
    div = new Div();
    div.dividend = 10;
    div.divisor = 3;
    div.perform();
    println("Quotient = ");
    println(div.quotient);
    println("Remainder = ");
    println(div.remainder);
  }
}

class B {
  Void f() {
    println(g(1, 2, 3, 4, 5, 6));
  }
  Int g(Int a, Int b, Int c, Int d, Int e, Int f) {
    Int result;
    result = a + b + c + d + e + f;
    println(result);
    return result;
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
