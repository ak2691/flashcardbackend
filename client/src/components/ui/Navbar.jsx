
import { Link } from 'react-router-dom';
export default function Navbar() {


    return (
        <nav className="bg-[#222831] px-6 py-4 shadow-lg">
            <div className="max-w-7xl mx-auto flex items-center">
                <Link
                    to="/"
                    className="bg-[#393E46] text-[#DFD0B8] px-6 py-2 rounded-lg font-medium hover:bg-[#948979] hover:text-[#222831] transition-colors duration-200"
                >
                    Home
                </Link>
                <Link
                    to="/login"
                    className="bg-[#393E46] text-[#DFD0B8] px-6 py-2 rounded-lg font-medium hover:bg-[#948979] hover:text-[#222831] transition-colors duration-200"
                >
                    Login
                </Link>

            </div>
        </nav>
    );
}