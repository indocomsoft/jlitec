class Main {
  Void main() {
    Fraction half;
    Fraction twoThirds;
    Fraction add;
    Fraction minus;
    Fraction times;
    Fraction divide;
    half = new Fraction();
    twoThirds = new Fraction();
    half.set(1, 2);
    twoThirds.set(2, 3);
    println("a =");
    half.print();
    println("b = ");
    twoThirds.print();
    println("a + b =");
    half.add(twoThirds).print();
    println("a - b =");
    half.minus(twoThirds).print();
    println("a * b =");
    half.times(twoThirds).print();
    println("a / b =");
    half.divide(twoThirds).print();
  }
}

class Fraction {
  Int numerator;
  Int denominator;

  Void set(Int numerator, Int denominator) {
    Int gcd;
    Int n;
    if (numerator == 0) {
      this.numerator = numerator;
      this.denominator = 1;
    } else {
      if (numerator >= 0) {
        n = numerator;
      } else {
        n = -numerator;
      }
      gcd = gcd(n, denominator);
      this.numerator = numerator / gcd;
      this.denominator = denominator / gcd;
    }
  }

  Fraction add(Fraction other) {
    Fraction result;
    Int td;
    Int od;
    Int n;
    Int d;
    result = new Fraction();
    td = this.denominator;
    od = other.denominator;
    n = this.numerator * od + other.numerator * td;
    d = td * od;
    result.set(n, d);
    return result;
  }

  Fraction minus(Fraction other) {
    Fraction result;
    Int td;
    Int od;
    Int n;
    Int d;
    result = new Fraction();
    td = this.denominator;
    od = other.denominator;
    n = this.numerator * od - other.numerator * td;
    d = td * od;
    result.set(n, d);
    return result;
  }

  Fraction times(Fraction other) {
    Fraction result;
    Int n;
    Int d;
    result = new Fraction();
    n = this.numerator * other.numerator;
    d = this.denominator * other.denominator;
    result.set(n, d);
    return result;
  }

  Fraction divide(Fraction other) {
    Fraction result;
    Int n;
    Int d;
    result = new Fraction();
    n = this.numerator * other.denominator;
    d = this.denominator * other.numerator;
    result.set(n, d);
    return result;
  }

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

  Void print() {
    println(this.numerator);
    if (this.numerator >= 0) {
      println("-");
    } else {
      println(" -");
    }
    println(this.denominator);
    println(null);
  }
}
