class Main {
  Void main() {
    Matrix a;
    Matrix b;
    a = new Matrix();
    b = new Matrix();
    a.set(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
    b.set(12, 23, 13, 29, 57, 42, 69, 23, 15, 47, 10, 49, 34, 32, 90, 27);
    println("a * b =");
    a.mult(b).print();
    /* Should be
       307,  376,  541,  330
       779,  952, 1269,  842
      1251, 1528, 1997, 1354
      1723, 2104, 2725, 1866
     */
  }
}

class Matrix {
  Int t11;
  Int t12;
  Int t13;
  Int t14;
  Int t21;
  Int t22;
  Int t23;
  Int t24;
  Int t31;
  Int t32;
  Int t33;
  Int t34;
  Int t41;
  Int t42;
  Int t43;
  Int t44;

  Void set(Int t11, Int t12, Int t13, Int t14, Int t21, Int t22, Int t23, Int t24, Int t31, Int t32, Int t33, Int t34, Int t41, Int t42, Int t43, Int t44) {
    this.t11 = t11;
    this.t12 = t12;
    this.t13 = t13;
    this.t14 = t14;
    this.t21 = t21;
    this.t22 = t22;
    this.t23 = t23;
    this.t24 = t24;
    this.t31 = t31;
    this.t32 = t32;
    this.t33 = t33;
    this.t34 = t34;
    this.t41 = t41;
    this.t42 = t42;
    this.t43 = t43;
    this.t44 = t44;
  }
  Matrix mult(Matrix o) {
    Matrix r;
    r = new Matrix();
    r.t11 = this.t11 * o.t11 + this.t12 * o.t21 + this.t13 * o.t31 + this.t14 * o.t41;
    r.t12 = this.t11 * o.t12 + this.t12 * o.t22 + this.t13 * o.t32 + this.t14 * o.t42;
    r.t13 = this.t11 * o.t13 + this.t12 * o.t23 + this.t13 * o.t33 + this.t14 * o.t43;
    r.t14 = this.t11 * o.t14 + this.t12 * o.t24 + this.t13 * o.t34 + this.t14 * o.t44;

    r.t21 = this.t21 * o.t11 + this.t22 * o.t21 + this.t23 * o.t31 + this.t24 * o.t41;
    r.t22 = this.t21 * o.t12 + this.t22 * o.t22 + this.t23 * o.t32 + this.t24 * o.t42;
    r.t23 = this.t21 * o.t13 + this.t22 * o.t23 + this.t23 * o.t33 + this.t24 * o.t43;
    r.t24 = this.t21 * o.t14 + this.t22 * o.t24 + this.t23 * o.t34 + this.t24 * o.t44;

    r.t31 = this.t31 * o.t11 + this.t32 * o.t21 + this.t33 * o.t31 + this.t34 * o.t41;
    r.t32 = this.t31 * o.t12 + this.t32 * o.t22 + this.t33 * o.t32 + this.t34 * o.t42;
    r.t33 = this.t31 * o.t13 + this.t32 * o.t23 + this.t33 * o.t33 + this.t34 * o.t43;
    r.t34 = this.t31 * o.t14 + this.t32 * o.t24 + this.t33 * o.t34 + this.t34 * o.t44;

    r.t41 = this.t41 * o.t11 + this.t42 * o.t21 + this.t43 * o.t31 + this.t44 * o.t41;
    r.t42 = this.t41 * o.t12 + this.t42 * o.t22 + this.t43 * o.t32 + this.t44 * o.t42;
    r.t43 = this.t41 * o.t13 + this.t42 * o.t23 + this.t43 * o.t33 + this.t44 * o.t43;
    r.t44 = this.t41 * o.t14 + this.t42 * o.t24 + this.t43 * o.t34 + this.t44 * o.t44;

    return r;
  }

  Void print() {
    println("[");
    println("[");
    println(t11);
    println(t12);
    println(t13);
    println(t14);
    println("]");
    println("[");
    println(t21);
    println(t22);
    println(t23);
    println(t24);
    println("]");
    println("[");
    println(t31);
    println(t32);
    println(t33);
    println(t34);
    println("]");
    println("[");
    println(t41);
    println(t42);
    println(t43);
    println(t44);
    println("]");
    println("]");
  }
}
