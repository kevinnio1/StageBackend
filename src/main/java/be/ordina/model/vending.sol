pragma solidity ^0.4.8;

contract Supplier {

    address owner;
    uint public priceInFinney;
    int public stock;

    modifier onlyOwner(){
        if(owner == msg.sender){
            _;
        }
    }

    event error(string message);
    event success(string message);
    event success(string message,uint value);

    /* this function is executed at initialization and sets the owner of the contract */
    function Supplier(uint price,int defaultStock) {
        owner = msg.sender;
        priceInFinney = price; 
        stock = defaultStock;
    }

    function withdrawAll() onlyOwner(){
        if(!owner.send(this.balance)){error("send back money Failed"); throw;}
    }

    function withdraw(int amountInFinney) onlyOwner(){
        if(!owner.send(uint256(amountInFinney * 1 finney))){error("send back money Failed"); throw;}
    }

    /* Function to recover the funds on the contract */
    function kill() onlyOwner()  {
        selfdestruct(owner);
    }

    function buyStock(int amount) payable returns (bool){

        if(stock - amount >= 0 && ((uint(amount) * priceInFinney)*1 finney) - 1 finney <= msg.value ){
            stock -= amount;
            return true;
        }else{
            if(!msg.sender.send(msg.value)){
                error("send back money Failed");
                throw;
            }
            error("stock is insufficient and/or not enough ether has been send");
            return false;
        }
    }

    function setPrice(uint newPriceInFinney){
        priceInFinney = newPriceInFinney;
        success("Price has been set",newPriceInFinney);
    }


    function () payable {
        error("Something went wrong");
        throw;
    }
}

contract VendingMachine {

    address owner;
    uint public finneyPrice;
    address supplier;
    address stakeholder;
    int public maxStock;
    int public minStock;
    int public stock;
    int public users;
    int public adminUsers;
    address[] internal accounts;
    address[] internal admins;
    address[] internal stakeholders;
    Supplier s ;

    event error(string message);
    event success(string message);
    event success(string message, uint value);

    modifier restrictAccessTo(address[] _collection){
        if(msg.sender == address(this)){_;return;}

        for(uint i = 0; i < _collection.length; i++) {
            if (_collection[i] == msg.sender) {
                _;
                return;
            }
        }

        if(msg.value > 0){
            if(!msg.sender.send(msg.value)) throw;
        }

    }

    modifier onlyOwner(){
        if(owner == msg.sender){
            _;
        }

    }

    modifier costs(uint _amount) {
        require(msg.value >= _amount * 1 finney);
        _;
        if (msg.value > _amount)
        if(!msg.sender.send(msg.value - _amount * 1 finney))throw;
    }


    /* this function is executed at initialization and sets the owner of the contract */
    function VendingMachine(int max, int min, uint price) {
        owner = msg.sender;
        maxStock = max;
        minStock = min;
        stock = maxStock;
        finneyPrice = price;
        addUser(msg.sender);
        adminUsers++;
        admins.push(msg.sender);
    }

    /* Function to recover the funds on the contract */
    function kill() onlyOwner()  {
        selfdestruct(owner);
    }

    function pay() payable restrictAccessTo(accounts) costs(finneyPrice ){
        if(stock>0){

            if(stakeholders.length > 0){
                divideProfit();
            }else{
                error("No stakeholders available");
            }

            stock--;

            if(stock == minStock){ this.resupply(maxStock-stock);}
        }else{
            error("Not enough stock to buy a product. Please wait for a resupply.");
            throw;
        }


    }

    function addStakeholder(address stakeholder) onlyOwner(){
        stakeholders.push(stakeholder);
        success("Stakeholder has been added");
    }

    function divideProfit() internal{
        uint profit = ((finneyPrice-s.priceInFinney()) * 1 finney);
        uint share = profit / stakeholders.length;

        for(uint x = 0; x < stakeholders.length; x++) {
            if(!stakeholders[x].send(share)) throw;
        }

        success("Share has been divided",share);
    }

    function resupply(int amount) restrictAccessTo(admins) payable returns (int) {
        if(amount<=0 && stock + amount > maxStock) throw;
        //msg.value minus the supplier price has been added here because the value from the current call isn't added yet to to contract balance if it is an automatic refill.
        uint weiToSend = (uint256(amount) * (s.priceInFinney() * 1 finney)) + msg.value-s.priceInFinney();
        if(!s.buyStock.value(weiToSend)(amount)) return stock;
        stock += amount;
        success("Stock has been resupplied",uint(amount));
        return stock;
    }

    function setSupplier(address a) onlyOwner() {
        supplier = a;
        s = Supplier(supplier);
        success("supplier has been set");
    }

    function setPrice(uint newPrice) onlyOwner() {
        finneyPrice = newPrice;
        success("Price has been set",newPrice);
    }

    function setMaxStock(int newStock) restrictAccessTo(admins) {
        if(stock>newStock || minStock>newStock) throw;
        maxStock= newStock;
        success("New max stock",uint(newStock));
    }

    function setMinStock(int newStock) restrictAccessTo(admins) {
        if(maxStock<newStock || stock< newStock) throw;
        minStock = newStock;
        success("New max stock",uint(minStock));
    }

    function addUser(address user){
        for(uint x = 0; x < accounts.length; x++) {
            if (accounts[x] == user) {
                throw;
            }
        }
        accounts.push(user);
        users++;
        success("User has been added");
    }

    function removeUser(address user) restrictAccessTo(admins){
        for(uint x = 0; x < accounts.length; x++) {
            if (accounts[x] == user) {
                //To fill the gap in the array
                accounts[x] == accounts[accounts.length-1];
                delete accounts[accounts.length-1];
                users--;
                success("Account has been removed from users");
                break;
            }
        }
    }

    function removeAdmin(address admin) restrictAccessTo(admins){
        for(uint x = 0; x < admins.length; x++) {
            if (admins[x] == admin) {
                //To fill the gap in the array
                admins[x] == admins[admins.length-1];
                delete admins[admins.length-1];
                adminUsers--;
                success("Account has been removed from admins");
                break;
            }
        }
    }

    function deleteAccount() restrictAccessTo(accounts){
        removeUser(msg.sender);
        removeAdmin(msg.sender);
    }

    function addAdmin(address admin) restrictAccessTo(admins){
        //add admin to the accounts list
        addUser(admin);
        for(uint x = 0; x < admins.length; x++) {
            if (admins[x] == admin) {
                throw;
            }
        }
        admins.push(admin);
        adminUsers++;

        success("Admin has been added");
    }


    function () {
        error("Something went wrong");
        throw; // throw reverts state to before call
    }
}