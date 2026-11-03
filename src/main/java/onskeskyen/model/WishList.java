package onskeskyen.model;

import java.util.ArrayList;

public class WishList {
   private ArrayList<Wish> wish;

   public ArrayList<Wish> getWish() {
      return wish;
   }

   public void setWish(ArrayList<Wish> wish) {
      this.wish = wish;
   }

   public WishList(ArrayList<Wish> wish){
       this.wish = wish;
   }


}
