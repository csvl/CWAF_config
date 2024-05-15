class Directive:
    def __init__(self, location, virtual_host, if_level, macro_name, macro_caller_instr, instr_number, name):
        self.Location = location
        self.VirtualHost = virtual_host
        self.IfLevel = if_level
        self.MacroName = macro_name
        self.MacroCallerInstr = macro_caller_instr
        self.InstrNumber = instr_number
        self.name = name

    def __repr__(self):
        return self.name
        #return f"Directive(Location={self.Location}, VirtualHost={self.VirtualHost}, IfLevel={self.IfLevel}, " \
               #f"MacroName={self.MacroName}, MacroCallerInstr={self.MacroCallerInstr}, InstrNumber={self.InstrNumber})"

    def __eq__(self, other):
        return (self.IfLevel, self.VirtualHost, self.Location, self.InstrNumber) == (other.IfLevel, other.VirtualHost, other.Location,other.InstrNumber)

    def __lt__(self, other):
        # Compare IfLevel first
        #import pdb; pdb.set_trace()
        if self.IfLevel != other.IfLevel:
            return self.IfLevel < other.IfLevel
        # Check if one of the Location is empty and the other not
        elif self.Location != other.Location and self.Location and not other.Location:    
            return False
        elif self.Location != other.Location and not self.Location and other.Location:
            return True
        elif self.VirtualHost != other.VirtualHost and self.VirtualHost and not other.VirtualHost:
            return False
        elif self.VirtualHost != other.VirtualHost and not self.VirtualHost and other.VirtualHost:
            return True
        else:
            return self.InstrNumber < other.InstrNumber
if __name__ == "__main__":        
    # Exemple d'utilisation

    #order1
    """directive1 = Directive("/loc1", "example.com", 0, "macro1", "caller1", 1,"vhost-loc1")
    directive2 = Directive(None, "example.com", 0, "macro2", "caller2", 2,"vhost-1")
    directive3 = Directive("/loc2", "example.com", 0, "macro3", "caller3", 3,"vhost-loc2")
    directive4 = Directive(None, "example.com", 0, "macro3", "caller3", 4,"vhost-2")
    directive5 = Directive("/loc1", None, 0, "macro3", "caller3", 5,"loc1")
    directive6 = Directive(None, None, 0, "macro3", "caller3", 6,"global-1")
    directive7 = Directive("/loc2", None, 0, "macro3", "caller3", 7,"loc2")
    directive8 = Directive(None, None, 0, "macro3", "caller3", 8,"global-2")"""

    #order2
    """directive1 = Directive(None, None, 2, "macro1", "caller1", 1,"if2")
    directive2 = Directive(None, None, 1, "macro2", "caller2", 2,"if")
    directive3 = Directive(None, None, 0, "macro3", "caller3", 3,"1")"""


    #order3
    directive1 = Directive("/loc1", "example.com", 2, "macro1", "caller1", 1, "vhost-loc1-if2")
    directive2 = Directive("/loc1", "example.com", 1, "macro2", "caller2", 2, "vhost-loc1-if")
    directive3 = Directive("/loc1", "example.com", 0, "macro3", "caller3", 3, "vhost-loc1")
    directive4 = Directive(None, "example.com", 2, "macro3", "caller3", 4, "vhost-1-if2")
    directive5 = Directive(None, "example.com", 1, "macro3", "caller3", 5, "vhost-1-if")
    directive6 = Directive(None, "example.com", 0, "macro3", "caller3", 6, "vhost1")
    directive7 = Directive("/loc2", "example.com", 0, "macro3", "caller3", 7, "vhost-loc2")
    directive8 = Directive(None, "example.com", 2, "macro3", "caller3", 8, "vhost-2-if2")
    directive9 = Directive(None, "example.com", 1, "macro1", "caller1", 9, "vhost-2-if")
    directive10 = Directive(None, "example.com", 0, "macro2", "caller2", 10, "vhost-2")

    directive11 = Directive("/loc2", None, 2, "macro3", "caller3", 11, "loc1-if2")
    directive12 = Directive("/loc2", None, 1, "macro3", "caller3", 12, "loc1-if")
    directive13 = Directive("/loc2", None, 0, "macro3", "caller3", 13, "loc1")

    directive14 = Directive(None, None, 2, "macro3", "caller3", 14, "global-1-if2")
    directive15 = Directive(None, None, 1, "macro3", "caller3", 15, "global-1-if")
    directive16 = Directive(None, None, 0, "macro3", "caller3", 16, "global-1")

    directive17 = Directive("/loc2", None, 2, "macro1", "caller1", 17, "loc2-if2")
    directive18 = Directive("/loc2", None, 1, "macro2", "caller2", 18, "loc2-if")
    directive19 = Directive("/loc2", None, 0, "macro3", "caller3", 19, "loc2")
    directive20 = Directive(None, None, 2, "macro3", "caller3", 20, "global-2-if2")
    directive21 = Directive(None, None, 1, "macro3", "caller3", 21, "global-2-if")
    directive22 = Directive(None, None, 0, "macro3", "caller3", 22, "global-2")
    #directive23 = Directive("/loc2", None, 0, "macro3", "caller3", 23, "loc2")
    #directive24 = Directive(None, None, 0, "macro3", "caller3", 24, "global-2")

    #directives = [directive1, directive2, directive3,directive4,directive5,directive6,directive7,directive8]
    #directives = [directive1, directive2, directive3]
    directives = [directive1, directive2, directive3, directive4, directive5, directive6, directive7, directive8, directive9, directive10, directive11, directive12, directive13, directive14, directive15, directive16, directive17, directive18, directive19, directive20, directive21, directive22]

    # Trie les directives en utilisant la fonction de comparaison définie dans la classe
    sorted_directives = sorted(directives)

    # Affiche le résultat
    for directive in sorted_directives:
        print(directive)
